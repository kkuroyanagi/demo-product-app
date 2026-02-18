import { ProLayout } from '@ant-design/pro-components';
import { DashboardOutlined, ShoppingOutlined, UnorderedListOutlined } from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';

const menuRoutes = {
  route: {
    routes: [
      {
        path: '/dashboard',
        name: 'ダッシュボード',
        icon: <DashboardOutlined />,
      },
      {
        path: '/products',
        name: '商品管理',
        icon: <ShoppingOutlined />,
        routes: [
          {
            path: '/products/list',
            name: '商品一覧',
            icon: <UnorderedListOutlined />,
          },
        ],
      },
    ],
  },
};

export const BasicLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();

  return (
    <ProLayout
      title="Demo App"
      layout="mix"
      fixSiderbar
      location={{ pathname: location.pathname }}
      {...menuRoutes}
      menuItemRender={(item, dom) => (
        <a onClick={() => item.path && navigate(item.path)}>{dom}</a>
      )}
    >
      <Outlet />
    </ProLayout>
  );
};
