package cn.wisestar.server.domain.dto;

import cn.wisestar.server.core.constant.ProjectModeEnum;
import lombok.Data;

/**
 * @author javahuang
 * @date 2022/9/11
 */
@Data
public class SelectTemplateRequest {

	private ProjectModeEnum mode;

}
