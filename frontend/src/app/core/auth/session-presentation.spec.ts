import { describe, expect, it } from 'vitest';
import { presentUserAgent } from './session-presentation';

describe('presentUserAgent', () => {
  it('presents common browsers and operating systems without exposing the raw value', () => {
    expect(presentUserAgent('Mozilla/5.0 (Windows NT 10.0) Chrome/151.0.0.0 Safari/537.36')).toBe(
      'Chrome 151 · Windows',
    );
    expect(presentUserAgent('Mozilla/5.0 (Macintosh) Version/18.1 Safari/605.1.15')).toBe(
      'Safari 18 · macOS',
    );
  });

  it('handles command-line and unknown clients safely', () => {
    expect(presentUserAgent('PowerShell/7.5 (Windows)')).toBe('PowerShell · Windows');
    expect(presentUserAgent(undefined)).toBe('unknown');
    expect(presentUserAgent('custom-client')).toBe('unknown');
  });
});
