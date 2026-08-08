package cn.wisestar.server.flow.domain.handler;

import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.util.List;

/**
 * 流程节点 JSON 类型处理器（MyBatis-Plus 自定义 TypeHandler）。
 *
 * <p>职责：实现 t_flow_entry.nodes 列（存 JSON 字符串）与 Java 对象
 * {@code List<FlowEntryNode>} 之间的自动转换：</p>
 * <ul>
 *   <li>读库时 {@link #parse(String)} 将 JSON 反序列化为节点列表；</li>
 *   <li>写库时 {@link #toJson(List)} 将节点列表序列化为 JSON 字符串。</li>
 * </ul>
 *
 * <p>所属流程环节：流程设计/持久化环节。实体 {@code FlowEntry.nodes} 字段标注
 * 了本处理器（@TableField(typeHandler = FlowEntryNodeTypeHandler.class)），
 * 使得"临时节点配置"可以随流程草稿一并 JSON 存储，无需单独建表。</p>
 *
 * <p>被谁调用：MyBatis-Plus 在读写 t_flow_entry.nodes 列时自动调用，无需业务代码直接调用。</p>
 *
 * <p>依赖什么：Spring 容器中的 Jackson {@link ObjectMapper}（通过 {@link ContextHelper}
 * 获取，保证与全局序列化配置一致）。</p>
 *
 * @author javahuang
 * @date 2022/1/12
 */
@MappedTypes({ Object.class })
@MappedJdbcTypes(JdbcType.VARCHAR)
public class FlowEntryNodeTypeHandler extends AbstractJsonTypeHandler<List<FlowEntryNode>> {

	/**
	 * 将数据库读取的 JSON 字符串反序列化为流程节点列表。
	 *
	 * @param json 数据库 t_flow_entry.nodes 列中的 JSON 字符串
	 * @return 解析后的节点列表 {@link FlowEntryNode}
	 * @throws RuntimeException JSON 格式非法时抛出（由 MyBatis 包装为持久化异常）
	 */
	@Override
	protected List<FlowEntryNode> parse(String json) {
		// 从 Spring 容器获取全局 ObjectMapper，保持日期、命名策略等序列化配置一致
		ObjectMapper mapper = ContextHelper.getBean(ObjectMapper.class);
		try {
			return mapper.readValue(json, new TypeReference<List<FlowEntryNode>>() {
			});
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 将流程节点列表序列化为 JSON 字符串写入数据库。
	 *
	 * @param obj 内存中的节点列表
	 * @return 序列化后的 JSON 字符串
	 * @throws RuntimeException 序列化失败时抛出
	 */
	@Override
	protected String toJson(List<FlowEntryNode> obj) {
		ObjectMapper mapper = ContextHelper.getBean(ObjectMapper.class);
		try {
			return mapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
