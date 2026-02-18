import { PageContainer } from '@ant-design/pro-components';
import { Card, Typography } from 'antd';

const { Title, Paragraph } = Typography;

export const DashboardPage = () => {
  return (
    <PageContainer>
      <Card>
        <Typography>
          <Title level={3}>Demo App へようこそ</Title>
          <Paragraph>
            このアプリケーションは、商品マスタの検索・一覧表示・Excel入出力をデモするWebアプリケーションです。
          </Paragraph>
          <Paragraph>
            左メニューの「商品管理 &gt; 商品一覧」から商品データの閲覧・操作を開始できます。
          </Paragraph>
        </Typography>
      </Card>
    </PageContainer>
  );
};
