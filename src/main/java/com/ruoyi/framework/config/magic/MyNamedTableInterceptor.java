package com.ruoyi.framework.config.magic;

import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.modules.db.inteceptor.NamedTableInterceptor;
import org.ssssssss.magicapi.modules.db.model.SqlMode;
import org.ssssssss.magicapi.modules.db.table.NamedTable;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 拦截请求
 */
@Component
@Slf4j
public class MyNamedTableInterceptor implements NamedTableInterceptor {

	/*
	 * 执行单表操作之前
	 */
	@Override
	public void preHandle(SqlMode sqlMode, NamedTable namedTable) {
		String tableName = namedTable.getTableName();

		// 判断是否为关联表，如果是关联表则不进行自动注入
		if (isJoinTable(tableName)) {
			return;
		}

		try {
			if (sqlMode == SqlMode.INSERT) {
				if (StpUtil.isLogin()) {
					// 插入时注入create_by列
					namedTable.column("create_by", StpUtil.getLoginId());
				}
				namedTable.column("create_time", DateUtil.date());
			} else if (sqlMode == SqlMode.UPDATE) {
				// 更新时注入update_by列
				if (StpUtil.isLogin()) {
					namedTable.column("update_by", StpUtil.getLoginId());
				}
				namedTable.column("update_time", DateUtil.date());
			}
		} catch (Exception e) {
			if (sqlMode == SqlMode.INSERT) {
				namedTable.column("create_time", DateUtil.date());
			} else if (sqlMode == SqlMode.UPDATE) {
				namedTable.column("update_time", DateUtil.date());
			}
		}

	}

	/**
	 * 判断是否为关联表
	 * 这里可以根据实际业务需求来定义关联表的判断规则
	 * 
	 * @param tableName 表名
	 * @return true表示是关联表，false表示是主表
	 */
	private boolean isJoinTable(String tableName) {
		if (StrUtil.isBlank(tableName)) {
			return false;
		}

		// 方法1：根据表名前缀或后缀判断
		// 例如：以"_rel"、"_relation"、"_mapping"结尾的表认为是关联表
		if (tableName.endsWith("_rel") ||
				tableName.endsWith("_relation") ||
				tableName.endsWith("_mapping") ||
				tableName.endsWith("_ref")) {
			return true;
		}

		// 方法2：根据表名中包含特定关键词判断
		// 例如：表名中包含"user_role"、"menu_role"等认为是关联表
		if (tableName.contains("_role_") ||
				tableName.contains("_menu_") ||
				tableName.contains("_dept_") ||
				tableName.contains("_user_")) {
			return true;
		}

		// 方法3：明确指定的关联表名
		switch (tableName) {
			case "sys_user_role":
			case "sys_role_menu":
			case "sys_role_dept":
			case "sys_user_post":
				return true;
			default:
				break;
		}

		// 默认认为是主表
		return false;
	}
}
