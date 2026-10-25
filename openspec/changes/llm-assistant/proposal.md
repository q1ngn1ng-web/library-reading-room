# Proposal: llm-assistant（聊天室 LLM 智能助手：查座/代约）【草案，待排期】

> 状态：方向已定（用户 2026-10-26 决策：优先"查座+代约"场景），未实现。

## 为什么

将项目从"管理系统"升级为"带 Agent 的后端系统"，与作者求职的 Agent 开发岗直接对口；且联动机制天然防滥用——**只有持有有效预约的在馆用户才能与助手对话**，调用者都是真实用户。

## 场景

1. **查空座**："3 楼还有空座吗？" → 助手调用查询函数返回实时空座列表（含推荐）
2. **代约座位**："帮我约 5 楼靠窗的位置" → 助手调预约函数（Function Calling）执行现有 `/reservations/reserve` 逻辑，返回结果；用户已有预约时如实告知失败原因
3. （二期）公告问答：闭馆时间/规则类问题，RAG 检索公告表

## 技术方案（本科生可达）

- Spring AI（spring-ai-openai-starter）+ 兼容 OpenAI 协议的国产模型 API（DeepSeek/Qwen，key 走环境变量 `LLM_API_KEY`）
- Function Calling 注册三个工具函数：`queryAvailableSeats(floor)`、`reserveSeat(seatId, startTime, endTime)`、`getNotices()`——**全部薄封装现有 Service 方法**，无新业务逻辑
- 助手身份：特殊 userId（如 0）+ name="[助手]"，仅在每个房间内广播回复，消息落 chat_message（type='assistant'）
- 入口：聊天室消息以 `@助手` 前缀触发；LLM 失败/超时降级为固定提示

## 非目标

- 不做多轮上下文记忆（一期单轮）、不做流式输出、不做 RAG（二期）

## 验收要点

1. "3楼有空座吗" → 返回真实空座数与编号
2. "帮我约5楼X号" → 产生真实预约（受并发防超卖约束），用户可见
3. 无预约用户无法使用（联动准入天然保证）；LLM API 不可用时聊天室不受影响
