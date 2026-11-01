import { useState, useEffect } from 'react';

// تایپ داده‌های پاسخ موفق
interface HealthResponse {
  status: string;
}

interface ReadyResponse {
  status: string;
  database: string;
}

// تایپ خطای استاندارد RFC 9457 طبق کد بک‌اند
interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  code?: string;
  detail?: string;
  correlation_id?: string;
}

export default function App() {
  const [healthStatus, setHealthStatus] = useState<string>('در حال بررسی...');
  const [dbStatus, setDbStatus] = useState<string>('در حال بررسی...');
  const [errorDetails, setErrorDetails] = useState<ProblemDetails | null>(null);
  const [loading, setLoading] = useState<boolean>(false);

  const BACKEND_URL = 'http://localhost:8085';

  const checkStatus = async () => {
    setLoading(true);
    setErrorDetails(null);

    // ۱. بررسی وضعیت Liveness (/health)
    try {
      const resHealth = await fetch(`${BACKEND_URL}/health`);
      if (resHealth.ok) {
        const data: HealthResponse = await resHealth.json();
        setHealthStatus(data.status);
      } else {
        setHealthStatus('DOWN');
      }
    } catch {
      setHealthStatus('UNREACHABLE');
    }

    // ۲. بررسی وضعیت Readiness و دیتابیس (/ready)
    try {
      const resReady = await fetch(`${BACKEND_URL}/ready`, {
        headers: {
          'X-Correlation-ID': crypto.randomUUID(), // ارسال شناسه ردیابی
        },
      });

      if (resReady.ok) {
        const data: ReadyResponse = await resReady.json();
        setDbStatus(data.database);
      } else {
        setDbStatus('DOWN');
        // خواندن خطای ساختاریافته RFC 9457
        const errJson: ProblemDetails = await resReady.json();
        setErrorDetails(errJson);
      }
    } catch (err: any) {
      setDbStatus('DOWN');
      setErrorDetails({
        title: 'Network Error',
        detail: 'امکان برقراری ارتباط با بک‌اند وجود ندارد. مطمئن شوید پورت ۸۰۸۵ فعال است.',
      });
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    checkStatus();
  }, []);

  return (
    <div style={containerStyle}>
      <h2 style={{ marginBottom: '24px' }}>داشبورد پایش وضعیت سرویس</h2>

      <div style={cardStyle}>
        <div style={rowStyle}>
          <span>وضعیت پروسس (Liveness):</span>
          <span style={badgeStyle(healthStatus === 'UP')}>{healthStatus}</span>
        </div>

        <div style={rowStyle}>
          <span>اتصال به دیتابیس (Readiness):</span>
          <span style={badgeStyle(dbStatus === 'UP')}>{dbStatus}</span>
        </div>

        <button onClick={checkStatus} disabled={loading} style={buttonStyle}>
          {loading ? 'در حال بررسی...' : 'بررسی مجدد'}
        </button>
      </div>

      {errorDetails && (
        <div style={errorCardStyle}>
          <h4 style={{ margin: '0 0 8px 0', color: '#b91c1c' }}>
            {errorDetails.title || 'خطا در دسترسی به دیتابیس'}
          </h4>
          <p style={{ margin: '4px 0', fontSize: '14px' }}>
            <strong>پیام خطا:</strong> {errorDetails.detail}
          </p>
          {errorDetails.correlation_id && (
            <p style={{ margin: '4px 0', fontSize: '13px', color: '#555' }}>
              <strong>Correlation ID:</strong> <code>{errorDetails.correlation_id}</code>
            </p>
          )}
          {errorDetails.code && (
            <p style={{ margin: '4px 0', fontSize: '13px', color: '#555' }}>
              <strong>کد خطا:</strong> <code>{errorDetails.code}</code>
            </p>
          )}
        </div>
      )}
    </div>
  );
}

// استایل‌های پایه برای تمیزی رابط کاربری
const containerStyle: React.CSSProperties = {
  fontFamily: 'sans-serif',
  maxWidth: '540px',
  margin: '40px auto',
  padding: '24px',
  direction: 'rtl',
};

const cardStyle: React.CSSProperties = {
  backgroundColor: '#fff',
  border: '1px solid #e5e7eb',
  borderRadius: '8px',
  padding: '20px',
  boxShadow: '0 2px 4px rgba(0,0,0,0.05)',
};

const rowStyle: React.CSSProperties = {
  display: 'flex',
  justifyContent: 'space-between',
  alignItems: 'center',
  marginBottom: '16px',
};

const badgeStyle = (isUp: boolean): React.CSSProperties => ({
  backgroundColor: isUp ? '#dcfce7' : '#fee2e2',
  color: isUp ? '#15803d' : '#b91c1c',
  padding: '4px 12px',
  borderRadius: '9999px',
  fontWeight: 'bold',
  fontSize: '13px',
});

const buttonStyle: React.CSSProperties = {
  width: '100%',
  padding: '10px',
  marginTop: '12px',
  backgroundColor: '#2563eb',
  color: '#fff',
  border: 'none',
  borderRadius: '6px',
  cursor: 'pointer',
  fontSize: '14px',
};

const errorCardStyle: React.CSSProperties = {
  marginTop: '20px',
  backgroundColor: '#fef2f2',
  border: '1px solid #fecaca',
  borderRadius: '8px',
  padding: '16px',
};