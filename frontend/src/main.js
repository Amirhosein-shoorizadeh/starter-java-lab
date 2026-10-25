document.querySelector('#app').innerHTML = `
  <div style="font-family: sans-serif; padding: 2rem; text-align: center;">
    <h1>LabBank Health Check</h1>
    <button id="check-btn" style="padding: 10px 20px; font-size: 16px; cursor: pointer;">بررسی سلامت بک‌اند</button>
    <p id="result" style="margin-top: 20px; font-weight: bold;">در انتظار بررسی...</p>
  </div>
`;

const resultElem = document.querySelector('#result');
const btn = document.querySelector('#check-btn');

async function checkHealth() {
  resultElem.textContent = 'در حال ارسال درخواست به بک‌اند...';
  try {
    const response = await fetch('http://127.0.0.1:8080/health');
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    const data = await response.json();
    resultElem.textContent = 'پاسخ دریافتی از سرور: ' + JSON.stringify(data);
    resultElem.style.color = 'green';
  } catch (error) {
    resultElem.textContent = 'خطا در ارتباط: ' + error.message;
    resultElem.style.color = 'red';
  }
}

btn.addEventListener('click', checkHealth);
checkHealth();