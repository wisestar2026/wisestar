package cn.wisestar.server.core.uitls;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 轻量级 excel 导出工具，换成 poi 打的包会多出十几 M，不能接受，-_-||
 *
 * @author javahuang
 * @date 2021/2/1
 */
@Slf4j
public class ExcelExporter {

	private Workbook workbook;

	private Map<String, List<String>> columnsBySheet = new ConcurrentHashMap<>();

	private Map<String, Integer> rowIndexBySheet = new ConcurrentHashMap<>();

	private ByteArrayOutputStream os = new ByteArrayOutputStream();

	ThreadLocal<Worksheet> localSheet = new ThreadLocal<>();

	protected static ThreadLocal<Integer> localOpenid = new ThreadLocal<>();

	public ExcelExporter() {
		this.workbook = new Workbook(os, "survey", "1.0");
	}

	public ExcelExporter(OutputStream outputStream) {
		this.workbook = new Workbook(outputStream, "survey", "1.0");
	}

	/**
	 * 新建工作表并初始化行索引，同时将其设为当前线程的工作表。
	 *
	 * @param sheetName 工作表名称
	 * @return 新建的工作表
	 */
	public Worksheet createSheet(String sheetName) {
		Worksheet worksheet = workbook.newWorksheet(sheetName);
		worksheet.fitToWidth((short) 10);
		worksheet.setFitToPage(true);
		localSheet.set(worksheet);
		rowIndexBySheet.put(sheetName, 1);
		return worksheet;
	}

	/**
	 * 在表头行写入列标题。
	 *
	 * @param columns 列标题列表
	 */
	public void createHeader(List<String> columns) {
		Worksheet sheet = localSheet.get();
		for (int i = 0; i < columns.size(); i++) {
			sheet.value(0, i, columns.get(i));
		}
	}

	/**
	 * 向当前工作表追加数据行，并按值类型写入单元格。
	 *
	 * @param rows 行数据集合，每个元素为一行中各单元格的值
	 */
	public void createRow(List<List<Object>> rows) {
		Worksheet sheet = localSheet.get();
		String sheetName = sheet.getName();
		int rowNum = rowIndexBySheet.get(sheetName);
		for (int r = 1; r <= rows.size(); r++) {
			List<Object> row = rows.get(r - 1);
			for (int c = 0; c < row.size(); c++) {
				Object value = row.get(c);
				if (value instanceof Integer) {
					sheet.value(r, c, (Number) value);
				}
				else if (value instanceof String) {
					sheet.value(r, c, (String) value);
				}
				else if (value != null) {
					sheet.value(r, c, value.toString());
				}
			}
			rowNum++;
		}
		rowIndexBySheet.put(sheetName, rowNum);
	}

	/**
	 * 结束写入并将 Excel 导出为字节输入流。
	 *
	 * @return 包含 Excel 内容的字节输入流；导出失败时返回 null
	 */
	public ByteArrayInputStream export() {
		try {
			this.workbook.finish();
			return new ByteArrayInputStream(this.os.toByteArray());
		}
		catch (IOException e) {
			log.error("Excel 导出失败", e);
		}
		return null;
	}

	/**
	 * 结束写入并将 Excel 输出到构造时指定的输出流。
	 */
	public void exportToStream() {
		try {
			this.workbook.finish();
		}
		catch (IOException e) {
			log.error("Excel 导出到输出流失败", e);
		}
	}

	@Data
	@Accessors(chain = true)
	public static class Builder {

		private String sheetName = "Sheet1";

		private List<String> columns;

		private List<List<Object>> rows;

		private OutputStream outputStream;

		/**
		 * 构建导出器：按配置创建实例，按需移除 openid 列并写入表头与数据。
		 *
		 * @return 已填充数据的 Excel 导出器
		 */
		public ExcelExporter build() {
			ExcelExporter excelExporter;
			if (this.outputStream != null) {
				excelExporter = new ExcelExporter(this.outputStream);
			}
			else {
				excelExporter = new ExcelExporter();
			}
			// 如果答案中不存在 openid，则删除该列
			int openidColumnIndex = this.columns.indexOf(SchemaHelper.getOpenIdColumnName());
			if (!Boolean.TRUE.equals(SchemaHelper.localOpenId.get()) && openidColumnIndex != -1) {
				this.columns.remove(openidColumnIndex);
				this.rows.forEach(row -> {
					row.remove(openidColumnIndex);
				});
				SchemaHelper.localOpenId.remove();
			}
			excelExporter.createSheet(this.sheetName);
			excelExporter.createRow(this.rows);
			excelExporter.createHeader(this.columns);

			return excelExporter;
		}

	}

}
