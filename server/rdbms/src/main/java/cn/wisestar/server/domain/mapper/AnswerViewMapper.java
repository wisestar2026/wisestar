package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.AnswerRequest;
import cn.wisestar.server.domain.dto.AnswerView;
import cn.wisestar.server.domain.model.Answer;
import org.mapstruct.Mapper;

/**
 * @author javahuang
 * @date 2021/10/6
 */
@Mapper
public interface AnswerViewMapper extends BaseModelMapper<AnswerRequest, AnswerView, Answer> {

}
