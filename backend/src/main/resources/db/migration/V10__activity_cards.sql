CREATE TABLE activity_cards (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    placement   VARCHAR(50)  NOT NULL,
    position    INT          NOT NULL,
    title       VARCHAR(100) NOT NULL,
    image_url   VARCHAR(500),
    link_url    VARCHAR(500) NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order  INT          NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_placement_position (placement, position),
    INDEX idx_placement_active (placement, is_active)
);

INSERT INTO activity_cards (placement, position, title, link_url, is_active, sort_order) VALUES
('homepage', 0, '限时秒杀', '/search?keyword=限时秒杀', TRUE, 0),
('homepage', 1, '百亿补贴', '/search?keyword=百亿补贴', TRUE, 1),
('homepage', 2, '官方好货', '/search?keyword=官方好货', TRUE, 2),
('homepage', 3, '新品首发', '/search?keyword=新品首发', TRUE, 3);
