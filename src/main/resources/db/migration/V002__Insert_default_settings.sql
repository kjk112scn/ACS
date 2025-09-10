-- 기본 설정 데이터 삽입
INSERT INTO settings (setting_key, setting_value, setting_type, description, is_system_setting) VALUES
('tracking.mode', 'Auto', 'STRING', '추적 모드 설정 (Auto/Manual)', false),
('tracking.interval', '60', 'INTEGER', '추적 주기 (초)', false),
('tracking.speed', '10', 'INTEGER', '추적 속도', false),
('tracking.accuracy', '1', 'INTEGER', '추적 정확도', false); 