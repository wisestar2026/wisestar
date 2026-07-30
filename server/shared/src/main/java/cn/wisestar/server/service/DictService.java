package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;

import java.util.List;

/**
 * @author javahuang
 * @date 2022/7/19
 */
public interface DictService {

	PaginationResponse<CommDictView> listDict(CommDictQuery query);

	void addDict(CommDictRequest request);

	void updateDict(CommDictRequest request);

	void deleteDict(String id);

	PaginationResponse<CommDictItemView> listDictItem(CommDictItemQuery query);

	void saveOrUpdateDictItem(CommDictItemRequest request);

	void deleteDictItem(String id);

	void importDictItem(CommDictItemRequest request);

	List<CommDictView> selectDict();

}
