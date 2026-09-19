package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.WordImageCandidateView;
import cn.wisestar.server.domain.dto.english.WordImageView;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 英语单词配图服务（候选抓取 / 确认入库 / 手动上传）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
public interface EnglishWordImageService {

	/**
	 * 为若干单词抓取候选图片（只读，不落库）。
	 *
	 * @param wordIds 单词 ID 列表
	 * @return 每个单词及其候选图片地址
	 */
	List<WordImageCandidateView> fetchCandidates(List<String> wordIds);

	/**
	 * 确认候选图：下载外部图片并存储到系统文件服务，回写单词图片地址。
	 *
	 * @param wordId   单词 ID
	 * @param imageUrl 候选图片地址
	 * @return 更新后的单词配图信息
	 */
	WordImageView confirmCandidate(String wordId, String imageUrl);

	/**
	 * 手动上传图片并回写单词图片地址。
	 *
	 * @param wordId 单词 ID
	 * @param file   图片文件
	 * @return 更新后的单词配图信息
	 */
	WordImageView uploadForWord(String wordId, MultipartFile file);

}
