===========================================================================
 AI Agent 后端 - Spring AI + DeepSeek + Tool Calling + Memory
===========================================================================

一、整体架构

React (前端)
   ↓ POST /api/ai/chat
AiController
   ↓
AgentService (接口)
   ↓
AgentServiceImpl (实现)
   ↓
Workflow (ReAct 工作流引擎，循环：LLM调用 → 工具执行 → LLM调用...)
   ├── PromptBuilder  - 拼接 SystemPrompt + History + Tool描述 + User消息
   ├── MemoryManager  - 对话历史持久化（存储到 ai_chat_history 表）
   ├── ToolRegistry   - 工具注册中心（管理所有 AgentTool）
   ├── LLMClient      - DeepSeek 模型客户端
   ├── MCPClient      - MCP 服务客户端（预留扩展）
   └── ReactAgent     - ReAct Agent 封装

二、模块说明

  com.aron.studio.ai
   ├── agent/ReactAgent.java       - ReAct 推理代理封装
   ├── config/
   │   ├── AiConfig.java           - ChatClient Bean 配置
   │   └── DemoDataInitializer.java - 启动时自动创建演示表 & 数据
   ├── controller/AiController.java - REST 接口
   ├── dto/
   │   ├── AgentChatRequest.java   - 聊天请求
   │   └── AgentChatResponse.java  - 聊天响应（含思考过程）
   ├── llm/LLMClient.java          - DeepSeek LLM 调用封装
   ├── mcp/MCPClient.java          - MCP 客户端（预留）
   ├── memory/
   │   ├── MemoryManager.java      - 对话历史管理
   │   └── entity/ChatHistory.java - 历史实体
   ├── prompt/PromptBuilder.java   - System + History + Tools + User 拼接
   ├── service/
   │   ├── AgentService.java       - Agent 服务接口
   │   └── impl/AgentServiceImpl.java - 服务实现
   ├── tools/
   │   ├── AgentTool.java          - 工具接口
   │   ├── ToolRegistry.java       - 工具注册中心
   │   └── impl/
   │       └── MysqlQueryTool.java - MySQL 查询工具
   └── worklow/Workflow.java       - ReAct 工作流引擎核心

三、接口说明

POST /api/ai/chat
  Request:  { "message": "查询user表有没有张三", "sessionId": "xxx" }
  Response: {
    "answer": "最终回答",
    "sessionId": "xxx",
    "thoughtProcess": [
      { "type": "THINK", "content": "..." },
      { "type": "TOOL_CALL", "toolName": "mysql_query", "content": "..." },
      { "type": "TOOL_RESULT", "content": "..." },
      { "type": "ANSWER", "content": "..." }
    ],
    "finished": true
  }

DELETE /api/ai/chat/{sessionId}  - 清空会话历史

四、对话流程示例（以查询张三为例）

1. 用户: 查询user表有没有张三
2. Agent 构建 Prompt:
   [SystemPrompt] + [History] + [ToolDescriptions] + [用户消息]
3. 第一次 LLM 调用：
   LLM 返回: TOOL_CALL: mysql_query
             ARGS: {"sql": "SELECT * FROM demo_user WHERE name='张三'"}
4. Agent 执行 MysqlQueryTool，得到查询结果
5. 第二次 LLM 调用（带工具结果）：
   Prompt = 原Prompt + [工具执行结果]
   LLM 返回: "数据库中存在张三，年龄28岁，邮箱zhangsan@example.com"
6. 返回最终回答给用户

五、演示数据库表（启动时自动创建）

1. demo_user    - 用户表（张三、李四、王五、赵六、张三丰）
2. demo_product - 产品表（笔记本电脑、机械键盘等）
3. demo_order   - 订单表（订单与用户、产品关联）
4. ai_chat_history - 对话历史表（自动创建）

六、扩展指南

1. 添加新工具：
   - 实现 AgentTool 接口
   - @Component 注册，自动被 ToolRegistry 发现

2. 修改 System Prompt：
   - 编辑 PromptBuilder.java 中的 SYSTEM_PROMPT 常量

3. 接入其他 LLM：
   - 修改 LLMClient.java 或增加新配置

4. 使用 MCP 服务：
   - 通过 MCPClient 调用外部 MCP 服务器

七、配置说明

application.yaml 中:
  spring.ai.deepseek.api-key: 设置 DeepSeek API Key
  spring.ai.deepseek.chat.options.model: deepseek-chat