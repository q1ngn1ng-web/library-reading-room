import axios from 'axios'
import { Message } from 'element-ui'

const baseURL = process.env.VUE_APP_BASE_API || "http://localhost:9090";

// 创建一个axios对象出来
const request = axios.create({
    baseURL: baseURL,
    timeout: 5000
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {
    // 任意组件或 request.js
    // console.log("当前 BASE_API:", process.env.VUE_APP_BASE_API);
    const isAdminRequest = config.url.startsWith('/admin');

    config.headers['Content-Type'] = 'application/json;charset=utf-8';

    const tokenKey = isAdminRequest
        ? localStorage.getItem('admin-token')
        : localStorage.getItem('user-token');
    const token = localStorage.getItem(tokenKey);
    if (token) {
        config.headers['token'] = token;
    }

    // config.headers['token'] = user.token;  // 设置请求头
    return config
}, error => {
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        // response.data即为后端返回的Result
        let res = response.data;
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        return res;
    },
    error => {
        // 增强错误处理
        console.error('API请求错误:', error);

        // 检查错误类型
        if (error.message === 'Network Error') {
            Message.error('无法连接到服务器，请确认后端服务已启动');
        } else if (error.code === 'ECONNABORTED') {
            Message.error('请求超时，请检查网络连接');
        } else if (error.response) {
            // 服务器返回了错误状态码
            const status = error.response.status;
            if (status === 401) {
                Message.error('未授权，请重新登录');
                // 可以在这里处理登出逻辑
            } else if (status === 403) {
                Message.error('没有权限访问该资源');
            } else if (status === 404) {
                Message.error('请求的资源不存在');
            } else if (status === 500) {
                Message.error('服务器内部错误');
            } else {
                Message.error(`请求失败: ${error.response.data.message || '未知错误'}`);
            }
        } else {
            Message.error('请求失败，请稍后重试');
        }

        return Promise.reject(error)
    }
)

export default request