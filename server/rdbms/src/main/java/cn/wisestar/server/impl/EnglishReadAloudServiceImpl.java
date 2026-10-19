package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.english.EnglishReadAloudView;
import cn.wisestar.server.domain.model.EnglishReadAloud;
import cn.wisestar.server.mapper.EnglishReadAloudMapper;
import cn.wisestar.server.service.EnglishReadAloudService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语课文跟读服务实现。
 *
 * @author wisestar
 * @date 2026/10/19
 */
@Slf4j
@Service
public class EnglishReadAloudServiceImpl implements EnglishReadAloudService {

	private final EnglishReadAloudMapper readAloudMapper;

	private final ObjectMapper objectMapper = new ObjectMapper();

	public EnglishReadAloudServiceImpl(EnglishReadAloudMapper readAloudMapper) {
		this.readAloudMapper = readAloudMapper;
	}

	@Override
	public List<EnglishReadAloudView> getPassages(String version, String grade, String term, String unit) {
		List<EnglishReadAloudView> result = new ArrayList<>();
		if (!StringUtils.hasText(version) || !StringUtils.hasText(grade) || !StringUtils.hasText(term)
				|| !StringUtils.hasText(unit)) {
			return result;
		}
		List<EnglishReadAloud> passages = readAloudMapper.selectList(Wrappers.<EnglishReadAloud>lambdaQuery()
				.eq(EnglishReadAloud::getVersion, version)
				.eq(EnglishReadAloud::getGrade, grade)
				.eq(EnglishReadAloud::getTerm, term)
				.eq(EnglishReadAloud::getUnit, unit)
				.orderByAsc(EnglishReadAloud::getSort));
		for (EnglishReadAloud passage : passages) {
			EnglishReadAloudView view = new EnglishReadAloudView();
			view.setId(passage.getId());
			view.setTitle(passage.getTitle());
			view.setSource(passage.getSource());
			view.setSort(passage.getSort());
			view.setLines(parseLines(passage));
			if (!view.getLines().isEmpty()) {
				result.add(view);
			}
		}
		return result;
	}

	private List<EnglishReadAloudView.Line> parseLines(EnglishReadAloud passage) {
		List<EnglishReadAloudView.Line> lines = new ArrayList<>();
		if (!StringUtils.hasText(passage.getContent())) {
			return lines;
		}
		try {
			JsonNode root = objectMapper.readTree(passage.getContent());
			if (!root.isArray()) {
				return lines;
			}
			for (JsonNode node : root) {
				String en = node.path("en").asText("");
				String zh = node.path("zh").asText("");
				if (StringUtils.hasText(en)) {
					EnglishReadAloudView.Line line = new EnglishReadAloudView.Line();
					line.setEn(en);
					line.setZh(zh);
					lines.add(line);
				}
			}
		}
		catch (Exception e) {
			log.warn("english read-aloud content parse failed, id={}: {}", passage.getId(), e.getMessage());
		}
		return lines;
	}

}
