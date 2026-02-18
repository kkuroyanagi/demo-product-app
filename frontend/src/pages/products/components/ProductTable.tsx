import { useEffect, useRef, useState } from 'react';
import { ProTable } from '@ant-design/pro-components';
import type { ProColumns, ActionType } from '@ant-design/pro-components';
import { Button, message } from 'antd';
import { DownloadOutlined, UploadOutlined } from '@ant-design/icons';
import dayjs from 'dayjs';
import type { Product, Category } from '../../../types/product';
import type { SearchParams } from '../../../services/productService';
import { productService } from '../../../services/productService';
import { ImportModal } from './ImportModal';

export const ProductTable = () => {
  const actionRef = useRef<ActionType>();
  const [categories, setCategories] = useState<Category[]>([]);
  const [importModalOpen, setImportModalOpen] = useState(false);
  const [searchParams, setSearchParams] = useState<SearchParams>({});

  useEffect(() => {
    productService.getCategories().then(setCategories);
  }, []);

  const categoryEnum = categories.reduce(
    (acc, cat) => {
      acc[cat.categoryName] = { text: cat.categoryName };
      return acc;
    },
    {} as Record<string, { text: string }>,
  );

  const columns: ProColumns<Product>[] = [
    {
      title: 'No',
      dataIndex: 'index',
      valueType: 'indexBorder',
      width: 60,
      search: false,
    },
    {
      title: '商品コード',
      dataIndex: 'productCode',
      sorter: true,
      width: 140,
      copyable: true,
      search: false,
    },
    {
      title: '商品名',
      dataIndex: 'productName',
      sorter: true,
      ellipsis: true,
      width: 250,
      search: false,
    },
    {
      title: 'カテゴリ',
      dataIndex: 'category',
      filters: true,
      onFilter: true,
      valueEnum: categoryEnum,
      width: 120,
      search: false,
    },
    {
      title: '単価',
      dataIndex: 'price',
      sorter: true,
      valueType: 'money',
      width: 120,
      align: 'right',
      search: false,
    },
    {
      title: '在庫数量',
      dataIndex: 'stockQuantity',
      sorter: true,
      width: 100,
      align: 'right',
      search: false,
    },
    {
      title: 'ステータス',
      dataIndex: 'status',
      filters: true,
      onFilter: true,
      valueEnum: {
        ACTIVE: { text: '有効', status: 'Success' },
        INACTIVE: { text: '無効', status: 'Default' },
        DISCONTINUED: { text: '廃止', status: 'Error' },
      },
      width: 100,
      search: false,
    },
    {
      title: '更新日時',
      dataIndex: 'updatedAt',
      sorter: true,
      valueType: 'dateTime',
      width: 180,
      search: false,
    },
    {
      title: 'キーワード',
      dataIndex: 'keyword',
      hideInTable: true,
    },
    {
      title: '単価（下限）',
      dataIndex: 'priceMin',
      valueType: 'money',
      hideInTable: true,
    },
    {
      title: '単価（上限）',
      dataIndex: 'priceMax',
      valueType: 'money',
      hideInTable: true,
    },
  ];

  const handleExport = async () => {
    try {
      const response = await productService.exportExcel(searchParams);
      const blob = new Blob([response.data], {
        type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `products_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
      a.click();
      window.URL.revokeObjectURL(url);
      message.success('Excelダウンロードが完了しました');
    } catch {
      // error handled by interceptor
    }
  };

  return (
    <>
      <ProTable<Product>
        columns={columns}
        actionRef={actionRef}
        rowKey="id"
        search={{ labelWidth: 'auto' }}
        request={async (params, sorter, filter) => {
          const sorterKey = Object.keys(sorter)[0];
          const sorterParam = sorterKey
            ? `${sorterKey},${sorter[sorterKey] === 'ascend' ? 'asc' : 'desc'}`
            : undefined;

          const apiParams: SearchParams = {
            keyword: params.keyword,
            category: (filter?.category?.[0] as string) ?? undefined,
            status: (filter?.status?.[0] as string) ?? undefined,
            priceMin: params.priceMin,
            priceMax: params.priceMax,
            current: params.current,
            pageSize: params.pageSize,
            sorter: sorterParam,
          };
          setSearchParams(apiParams);

          const result = await productService.search(apiParams);
          return {
            data: result.data,
            total: result.total,
            success: result.success,
          };
        }}
        pagination={{
          defaultPageSize: 20,
          showSizeChanger: true,
          pageSizeOptions: ['10', '20', '50', '100'],
        }}
        toolBarRender={() => [
          <Button key="export" icon={<DownloadOutlined />} onClick={handleExport}>
            Excelダウンロード
          </Button>,
          <Button
            key="import"
            type="primary"
            icon={<UploadOutlined />}
            onClick={() => setImportModalOpen(true)}
          >
            Excelアップロード
          </Button>,
        ]}
        dateFormatter="string"
      />
      <ImportModal
        open={importModalOpen}
        onClose={() => setImportModalOpen(false)}
        onSuccess={() => actionRef.current?.reload()}
      />
    </>
  );
};
