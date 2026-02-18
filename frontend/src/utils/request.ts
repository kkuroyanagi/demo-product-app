import axios from 'axios';
import { message } from 'antd';

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

request.interceptors.response.use(
  (response) => response,
  (error) => {
    const msg = error.response?.data?.message || error.message || 'リクエストエラーが発生しました';
    message.error(msg);
    return Promise.reject(error);
  },
);

export { request };
