export interface SystemStatus {
  status: 'UP' | 'DOWN';
  postgres: 'UP' | 'DOWN';
  redis: 'UP' | 'DOWN';
}
