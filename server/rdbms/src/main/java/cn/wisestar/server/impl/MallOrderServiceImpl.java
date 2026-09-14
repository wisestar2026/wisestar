package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.mall.MallOrderRequest;
import cn.wisestar.server.domain.dto.mall.MallOrderView;
import cn.wisestar.server.domain.model.MallGoods;
import cn.wisestar.server.domain.model.MallOrder;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.mapper.MallGoodsMapper;
import cn.wisestar.server.mapper.MallOrderMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.MallOrderService;
import cn.wisestar.server.service.StudentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 学币兑换订单服务实现。
 *
 * <p><b>兑换流程</b>：学员在商城对商品发起兑换 → 校验学币余额 → 生成订单（含随机 6 位核销码）
 * 并写入负向学币流水（暂时扣除）→ 老师凭核销码在核销页完成订单（学币正式扣除），记录保留。</p>
 *
 * @author wisestar
 * @date 2026/9/14
 */
@Service
@RequiredArgsConstructor
public class MallOrderServiceImpl extends BaseService<MallOrderMapper, MallOrder> implements MallOrderService {

	private final MallGoodsMapper mallGoodsMapper;

	private final StudentMapper studentMapper;

	private final StudentService studentService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MallOrderView create(MallOrderRequest request) {
		if (request == null || !StringUtils.hasText(request.getGoodsId())) {
			throw new ValidationException("请选择要兑换的商品");
		}
		String studentId = SecurityContextUtils.getUserId();
		Student student = studentId == null ? null : studentMapper.selectById(studentId);
		if (student == null) {
			throw new ValidationException("当前用户不是学员");
		}
		MallGoods goods = mallGoodsMapper.selectById(request.getGoodsId());
		if (goods == null || !Integer.valueOf(1).equals(goods.getStatus())) {
			throw new ValidationException("商品不存在或已下架");
		}
		int price = goods.getPoints() == null ? 0 : goods.getPoints();
		if (price <= 0) {
			throw new ValidationException("商品所需学币配置有误");
		}
		if (studentService.coinBalance(studentId) < price) {
			throw new ValidationException("学币不足，无法兑换");
		}
		MallOrder order = new MallOrder();
		order.setStudentId(studentId);
		order.setStudentNo(student.getStudentNo());
		order.setStudentName(student.getName());
		order.setGoodsId(goods.getId());
		order.setGoodsName(goods.getName());
		order.setGoodsImage(goods.getImageUrl());
		order.setCoins(price);
		order.setVerifyCode(nextVerifyCode());
		order.setStatus(0);
		save(order);
		// 暂时扣除学币（核销完成后即为正式扣除；订单记录与负向流水一一对应）
		studentService.deductCoins(studentId, price,
				"商城兑换：" + goods.getName() + "（核销码 " + order.getVerifyCode() + "）");
		return toView(order);
	}

	@Override
	public List<MallOrderView> myOrders() {
		String studentId = SecurityContextUtils.getUserId();
		if (studentId == null || studentMapper.selectById(studentId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return this.baseMapper.selectList(Wrappers.<MallOrder>lambdaQuery()
						.eq(MallOrder::getStudentId, studentId)
						.orderByDesc(MallOrder::getCreateAt))
				.stream().map(this::toView).collect(Collectors.toList());
	}

	@Override
	public List<MallOrderView> listOrders(MallOrderRequest request) {
		Integer status = request == null ? null : request.getStatus();
		String keyword = request == null ? null : request.getKeyword();
		LambdaQueryWrapper<MallOrder> query = Wrappers.<MallOrder>lambdaQuery()
				.eq(status != null, MallOrder::getStatus, status)
				.orderByAsc(MallOrder::getStatus)
				.orderByDesc(MallOrder::getCreateAt);
		if (StringUtils.hasText(keyword)) {
			String kw = keyword.trim();
			query.and(w -> w.like(MallOrder::getStudentName, kw).or().eq(MallOrder::getVerifyCode, kw));
		}
		return this.baseMapper.selectList(query).stream().map(this::toView).collect(Collectors.toList());
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public MallOrderView verify(MallOrderRequest request) {
		MallOrder target;
		if (request != null && StringUtils.hasText(request.getId())) {
			target = getById(request.getId());
			if (target == null) {
				throw new ValidationException("订单不存在");
			}
		} else if (request != null && StringUtils.hasText(request.getVerifyCode())) {
			String code = request.getVerifyCode().trim();
			target = this.baseMapper.selectList(Wrappers.<MallOrder>lambdaQuery()
							.eq(MallOrder::getVerifyCode, code)
							.eq(MallOrder::getStatus, 0))
					.stream().findFirst().orElse(null);
			if (target == null) {
				boolean used = this.baseMapper.selectCount(Wrappers.<MallOrder>lambdaQuery()
						.eq(MallOrder::getVerifyCode, code)) > 0;
				throw new ValidationException(used ? "该核销码已完成核销" : "核销码不存在");
			}
		} else {
			throw new ValidationException("请填写核销码");
		}
		if (Integer.valueOf(1).equals(target.getStatus())) {
			throw new ValidationException("该订单已完成核销");
		}
		target.setStatus(1);
		target.setVerifyAt(new Date());
		target.setVerifyBy(SecurityContextUtils.getUserId());
		updateById(target);
		return toView(target);
	}

	/**
	 * 生成未被待核销订单占用的随机 6 位核销码。
	 */
	private String nextVerifyCode() {
		for (int i = 0; i < 50; i++) {
			String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
			Long exists = this.baseMapper.selectCount(Wrappers.<MallOrder>lambdaQuery()
					.eq(MallOrder::getVerifyCode, code)
					.eq(MallOrder::getStatus, 0));
			if (exists == null || exists == 0L) {
				return code;
			}
		}
		throw new ValidationException("核销码生成失败，请重试");
	}

	private MallOrderView toView(MallOrder order) {
		MallOrderView view = new MallOrderView();
		view.setId(order.getId());
		view.setStudentId(order.getStudentId());
		view.setStudentNo(order.getStudentNo());
		view.setStudentName(order.getStudentName());
		view.setGoodsId(order.getGoodsId());
		view.setGoodsName(order.getGoodsName());
		view.setGoodsImage(order.getGoodsImage());
		view.setCoins(order.getCoins());
		view.setVerifyCode(order.getVerifyCode());
		view.setStatus(order.getStatus());
		view.setVerifyAt(order.getVerifyAt());
		view.setVerifyBy(order.getVerifyBy());
		view.setCreateAt(order.getCreateAt());
		return view;
	}

}
