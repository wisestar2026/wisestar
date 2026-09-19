package cn.wisestar.server.impl;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 基于字节数组的 {@link MultipartFile} 实现。
 *
 * <p>用于把程序下载到的图片字节（如候选配图）包装成 MultipartFile，
 * 复用 {@code FileService.upload} 落盘与元数据入库。</p>
 *
 * @author wisestar
 * @date 2026/9/18
 */
public class ByteArrayMultipartFile implements MultipartFile {

	private final String name;

	private final String originalFilename;

	private final String contentType;

	private final byte[] bytes;

	public ByteArrayMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
		this.name = name;
		this.originalFilename = originalFilename;
		this.contentType = contentType;
		this.bytes = bytes == null ? new byte[0] : bytes;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public boolean isEmpty() {
		return bytes.length == 0;
	}

	@Override
	public long getSize() {
		return bytes.length;
	}

	@Override
	public byte[] getBytes() {
		return bytes.clone();
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public void transferTo(File dest) throws IOException {
		Files.write(dest.toPath(), bytes);
	}

}
