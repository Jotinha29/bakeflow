export function presentUserAgent(userAgent?: string): string {
  if (!userAgent?.trim()) return 'unknown';
  const browser = browserName(userAgent);
  const system = operatingSystem(userAgent);
  return [browser, system].filter(Boolean).join(' · ') || 'unknown';
}

function browserName(value: string): string {
  const edge = value.match(/Edg\/(\d+)/);
  if (edge) return `Edge ${edge[1]}`;
  const chrome = value.match(/(?:Chrome|CriOS)\/(\d+)/);
  if (chrome) return `Chrome ${chrome[1]}`;
  const firefox = value.match(/(?:Firefox|FxiOS)\/(\d+)/);
  if (firefox) return `Firefox ${firefox[1]}`;
  const safari = value.match(/Version\/(\d+).*Safari/);
  if (safari) return `Safari ${safari[1]}`;
  if (/PowerShell/i.test(value)) return 'PowerShell';
  if (/curl/i.test(value)) return 'cURL';
  return '';
}

function operatingSystem(value: string): string {
  if (/Windows/i.test(value)) return 'Windows';
  if (/Android/i.test(value)) return 'Android';
  if (/iPhone|iPad|iOS/i.test(value)) return 'iOS';
  if (/Mac OS|Macintosh/i.test(value)) return 'macOS';
  if (/Linux/i.test(value)) return 'Linux';
  return '';
}
