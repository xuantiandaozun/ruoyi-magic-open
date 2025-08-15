package com.ruoyi.framework.config.magic;

import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.modules.db.inteceptor.NamedTableInterceptor;
import org.ssssssss.magicapi.modules.db.model.SqlMode;
import org.ssssssss.magicapi.modules.db.table.NamedTable;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;

/**
 * 拦截请求
 */
@Component
public class MyNamedTableInterceptor implements NamedTableInterceptor {

	/*
	 * 执行单表操作之前
	 */
	@Override
	public void preHandle(SqlMode sqlMode, NamedTable namedTable) {
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
	}
}
