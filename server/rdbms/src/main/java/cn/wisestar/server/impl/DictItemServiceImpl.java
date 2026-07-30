package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.CommDictItem;
import cn.wisestar.server.mapper.CommDictItemMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.DictItemService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author javahuang
 * @date 2022/7/20
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DictItemServiceImpl extends BaseService<CommDictItemMapper, CommDictItem> implements DictItemService {

}
