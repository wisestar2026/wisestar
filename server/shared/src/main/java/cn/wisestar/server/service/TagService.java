package cn.wisestar.server.service;

import cn.wisestar.server.core.constant.TagCategoryEnum;
import cn.wisestar.server.domain.dto.SelectTagRequest;

import java.util.Set;

/**
 * @author javahuang
 * @date 2022/4/28
 */
public interface TagService {

	void batchAddTag(String entityId, TagCategoryEnum category, String[] tagArr);

	void deleteTagByEntryId(String entityId);

	Set<String> selectTag(SelectTagRequest request);

}
