/*
 Navicat Premium Dump SQL

 Source Server         : 个人阿里云数据库
 Source Server Type    : MySQL
 Source Server Version : 80036 (8.0.36)
 Source Host           : rm-bp1k7j3y96w020tz1eo.mysql.rds.aliyuncs.com:3306
 Source Schema         : ry-vue

 Target Server Type    : MySQL
 Target Server Version : 80036 (8.0.36)
 File Encoding         : 65001

 Date: 24/10/2025 17:43:30
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for github_trending
-- ----------------------------
DROP TABLE IF EXISTS `github_trending`;
CREATE TABLE `github_trending`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '仓库标题',
  `owner` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '仓库作者',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '仓库描述',
  `url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '仓库地址',
  `language` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '仓库语言',
  `trending_days` int NULL DEFAULT 0 COMMENT '总上榜天数',
  `continuous_trending_days` int NULL DEFAULT 0 COMMENT '连续上榜天数',
  `first_trending_date` date NULL DEFAULT NULL COMMENT '首次上榜日期',
  `last_trending_date` date NULL DEFAULT NULL COMMENT '最后一次上榜日期',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '跟新时间',
  `is_tran_des` int NOT NULL DEFAULT 0 COMMENT '是否翻译描述',
  `stars_count` int NULL DEFAULT 0 COMMENT '项目的 star 数量',
  `forks_count` int NULL DEFAULT 0 COMMENT '项目的 fork 数量',
  `open_issues_count` int NULL DEFAULT 0 COMMENT '开放问题数量',
  `github_created_at` datetime NULL DEFAULT NULL COMMENT '仓库创建时间',
  `github_updated_at` datetime NULL DEFAULT NULL COMMENT '仓库最后更新时间',
  `readme_path` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT 'readme 文件路径',
  `readme_updated_at` datetime NULL DEFAULT NULL COMMENT 'readme 文件跟新日期',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_at` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `is_need_update` int NOT NULL DEFAULT 0 COMMENT '是否需要跟新项目',
  `ai_readme_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT 'ai翻译后的readme文件',
  `rep_value` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '仓库价值:普通/值得关注/值得收藏/商业价值',
  `promotion_article` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL COMMENT '仓库推广文章',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '备注',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '更新者',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NULL DEFAULT NULL COMMENT '创建者',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `url`(`url` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_bin COMMENT = 'github流行榜单' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
