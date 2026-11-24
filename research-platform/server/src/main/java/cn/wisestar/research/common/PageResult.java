package cn.wisestar.research.common;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageResult<T> {

	private long total;

	private List<T> list;

	private long current;

	private long pageSize;

}
