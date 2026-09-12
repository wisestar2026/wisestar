package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.student.SectionPassResult;
import cn.wisestar.server.domain.model.SectionPass;
import cn.wisestar.server.mapper.SectionPassMapper;
import cn.wisestar.server.service.SectionPassService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 小节通关服务实现。
 *
 * <p>按 (user_id, section_id) 取唯一记录：不存在则插入，存在则累加尝试次数、
 * 刷新历史最佳（best_rate/best_stars/best_score 取 max）；passed 由 false 变 true 时
 * 记录首次通关时间，此后保持 true。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectionPassServiceImpl implements SectionPassService {

	private final SectionPassMapper sectionPassMapper;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public SectionPassResult record(String userId, String sectionId, int rate, double score, double totalScore,
			int passRate, boolean unlockedNext) {
		SectionPassResult result = new SectionPassResult();
		int normalizedRate = Math.max(0, Math.min(100, rate));
		int stars = starOf(normalizedRate);
		boolean passed = normalizedRate >= passRate;
		Date now = new Date();

		result.setRate(normalizedRate);
		result.setStars(stars);
		result.setPassed(passed);
		result.setPassRate(passRate);
		result.setUnlockedNext(passed && unlockedNext);

		SectionPass existing = sectionPassMapper.selectOne(Wrappers.<SectionPass>lambdaQuery()
				.eq(SectionPass::getUserId, userId)
				.eq(SectionPass::getSectionId, sectionId)
				.last("limit 1"));

		if (existing == null) {
			SectionPass record = new SectionPass();
			record.setUserId(userId);
			record.setSectionId(sectionId);
			record.setBestRate(normalizedRate);
			record.setBestScore(score);
			record.setStars(stars);
			record.setPassed(passed);
			record.setAttemptCount(1);
			record.setFirstPassAt(passed ? now : null);
			record.setLastAttemptAt(now);
			sectionPassMapper.insert(record);
			result.setFirstPass(passed);
			result.setBestRate(normalizedRate);
			result.setBestStars(stars);
			return result;
		}

		boolean wasPassed = Boolean.TRUE.equals(existing.getPassed());
		int bestRate = Math.max(existing.getBestRate() == null ? 0 : existing.getBestRate(), normalizedRate);
		int bestStars = Math.max(existing.getStars() == null ? 0 : existing.getStars(), stars);
		double bestScore = Math.max(existing.getBestScore() == null ? 0 : existing.getBestScore(), score);

		existing.setBestRate(bestRate);
		existing.setBestScore(bestScore);
		existing.setStars(bestStars);
		existing.setAttemptCount((existing.getAttemptCount() == null ? 0 : existing.getAttemptCount()) + 1);
		existing.setLastAttemptAt(now);
		if (passed && !wasPassed) {
			existing.setPassed(Boolean.TRUE);
			existing.setFirstPassAt(now);
		}
		sectionPassMapper.updateById(existing);

		result.setFirstPass(passed && !wasPassed);
		result.setBestRate(bestRate);
		result.setBestStars(bestStars);
		return result;
	}

	/** 正确率 → 星级（与学员端既有分档一致） */
	private int starOf(int rate) {
		if (rate >= 80) {
			return 5;
		}
		if (rate >= 60) {
			return 4;
		}
		if (rate >= 40) {
			return 3;
		}
		if (rate >= 20) {
			return 2;
		}
		return rate > 0 ? 1 : 0;
	}

}
