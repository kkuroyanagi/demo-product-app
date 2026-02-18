import { useState } from 'react';
import { Modal, Upload, Table, Alert, Space, Typography } from 'antd';
import { InboxOutlined } from '@ant-design/icons';
import type { UploadFile } from 'antd';
import type { ImportResult, ImportError } from '../../../types/product';
import { productService } from '../../../services/productService';

const { Dragger } = Upload;
const { Text } = Typography;

interface ImportModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess: () => void;
}

export const ImportModal = ({ open, onClose, onSuccess }: ImportModalProps) => {
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);

  const handleClose = () => {
    setResult(null);
    onClose();
  };

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const res = await productService.importExcel(file);
      setResult(res);
      if (res.errorCount === 0) {
        onSuccess();
      }
    } finally {
      setUploading(false);
    }
  };

  const errorColumns = [
    { title: '行番号', dataIndex: 'row', key: 'row', width: 80 },
    { title: '項目', dataIndex: 'field', key: 'field', width: 120 },
    { title: 'エラー内容', dataIndex: 'message', key: 'message' },
  ];

  return (
    <Modal
      title="Excelアップロード"
      open={open}
      onCancel={handleClose}
      footer={null}
      width={640}
      destroyOnClose
    >
      <Dragger
        accept=".xlsx"
        maxCount={1}
        showUploadList={false}
        customRequest={({ file, onSuccess: onUploadSuccess, onError }) => {
          handleUpload(file as File)
            .then(() => onUploadSuccess?.('ok'))
            .catch((err) => onError?.(err));
        }}
        disabled={uploading}
      >
        <p className="ant-upload-drag-icon">
          <InboxOutlined />
        </p>
        <p className="ant-upload-text">ここにExcelファイルをドロップ、またはクリックして選択</p>
        <p className="ant-upload-hint">対応フォーマット: .xlsx</p>
      </Dragger>

      {result && (
        <Space direction="vertical" style={{ width: '100%', marginTop: 16 }}>
          <Alert
            type={result.errorCount === 0 ? 'success' : 'warning'}
            message={
              <Space>
                <Text>
                  処理件数: {result.totalRows}件 / 新規: {result.insertedCount}件 / 更新:{' '}
                  {result.updatedCount}件
                </Text>
                {result.errorCount > 0 && (
                  <Text type="danger">エラー: {result.errorCount}件</Text>
                )}
              </Space>
            }
          />
          {result.errors.length > 0 && (
            <Table<ImportError>
              columns={errorColumns}
              dataSource={result.errors}
              rowKey={(record) => `${record.row}-${record.field}`}
              size="small"
              pagination={{ pageSize: 10 }}
            />
          )}
        </Space>
      )}
    </Modal>
  );
};
