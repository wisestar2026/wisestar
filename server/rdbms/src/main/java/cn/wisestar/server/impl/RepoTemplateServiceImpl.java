package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.RepoTemplate;
import cn.wisestar.server.mapper.RepoTemplateMapper;
import cn.wisestar.server.service.BaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author javahuang
 * @date 2022/4/29
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class RepoTemplateServiceImpl extends BaseService<RepoTemplateMapper, RepoTemplate> {

}
