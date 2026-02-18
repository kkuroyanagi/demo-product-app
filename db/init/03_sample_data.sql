-- ============================================================
-- 03_sample_data.sql
-- サンプル商品データ生成（3,000件）
--
-- PL/pgSQL の DO ブロックで手続き的に生成する。
-- カテゴリごとに600件、合計3,000件を INSERT する。
-- 商品名はカテゴリ別のプレフィックス・サフィックス配列から
-- 組み合わせて生成し、現実的な日本語商品名を作る。
-- ============================================================

DO $$
DECLARE
    -- ループ変数
    i              INTEGER;
    rec_num        INTEGER := 0;   -- 通し番号 (1..3000)
    cat_idx        INTEGER;        -- カテゴリインデックス (1..5)
    item_in_cat    INTEGER;        -- カテゴリ内連番 (1..600)

    -- カテゴリ
    v_category     VARCHAR(50);
    categories     TEXT[] := ARRAY['電子機器', '食品', '衣類', '書籍', '日用品'];

    -- 商品名パーツ（カテゴリごと）
    -- 電子機器
    elec_prefix    TEXT[] := ARRAY['スマート', 'ワイヤレス', 'ポータブル', 'プロ', 'ミニ', 'ハイスペック', '超薄型', '高性能', 'コンパクト', 'デジタル'];
    elec_noun      TEXT[] := ARRAY['イヤホン', 'スピーカー', 'キーボード', 'マウス', 'モニター', 'タブレット', 'カメラ', 'プリンター', 'ルーター', 'USBハブ', 'ウェブカメラ', '充電器'];
    elec_suffix    TEXT[] := ARRAY[' Pro', ' Lite', ' Max', ' SE', ' Plus', ' X1', ' V2', ' Air', ' Neo', ' Z'];

    -- 食品
    food_prefix    TEXT[] := ARRAY['国産', '有機', '特選', '厳選', '手作り', '本格', '天然', '無添加', 'プレミアム', '北海道'];
    food_noun      TEXT[] := ARRAY['緑茶', '味噌', '醤油', 'うどん', 'そば', 'ラーメン', 'カレー', 'チョコレート', 'クッキー', 'ジュース', 'ジャム', 'はちみつ'];
    food_suffix    TEXT[] := ARRAY[' セット', ' 詰合せ', ' 大容量', ' ギフト', ' お徳用', ' 3個入', ' 5袋入', ' 限定品', ' 特大', ' ミニ'];

    -- 衣類
    cloth_prefix   TEXT[] := ARRAY['オーガニック', 'ストレッチ', 'リネン', 'シルク', 'カジュアル', 'フォーマル', 'ヴィンテージ', 'モダン', 'クラシック', 'スポーツ'];
    cloth_noun     TEXT[] := ARRAY['Tシャツ', 'ジャケット', 'パンツ', 'ワンピース', 'スカート', 'コート', 'セーター', 'カーディガン', 'シャツ', 'ベスト', 'パーカー', 'ブラウス'];
    cloth_suffix   TEXT[] := ARRAY[' S', ' M', ' L', ' XL', ' ホワイト', ' ブラック', ' ネイビー', ' グレー', ' ベージュ', ' ブルー'];

    -- 書籍
    book_prefix    TEXT[] := ARRAY['入門', '実践', '完全ガイド', '図解', '最新版', '決定版', 'よくわかる', '基礎から学ぶ', 'プロが教える', '世界一やさしい'];
    book_noun      TEXT[] := ARRAY['プログラミング', 'データサイエンス', 'ビジネス戦略', '英会話', '日本史', '料理レシピ', '写真撮影', '経済学', 'マーケティング', 'デザイン', '心理学', '数学'];
    book_suffix    TEXT[] := ARRAY[' 入門編', ' 応用編', ' 第2版', ' 第3版', ' 上巻', ' 下巻', ' 完全版', ' 改訂版', ' 新装版', ' ワークブック'];

    -- 日用品
    daily_prefix   TEXT[] := ARRAY['エコ', '大容量', '除菌', '消臭', '速乾', '抗菌', 'ナチュラル', 'やさしい', 'プロ仕様', '高吸収'];
    daily_noun     TEXT[] := ARRAY['洗剤', 'ハンドソープ', 'ティッシュ', 'タオル', 'スポンジ', '掃除シート', 'ゴミ袋', '歯ブラシ', 'シャンプー', 'ボディソープ', 'ラップ', '食器洗い洗剤'];
    daily_suffix   TEXT[] := ARRAY[' 詰替え', ' 業務用', ' お得パック', ' 3本セット', ' ミニ', ' ジャンボ', ' 無香料', ' フローラル', ' 携帯用', ' 泡タイプ'];

    -- 説明文テンプレート
    elec_desc      TEXT[] := ARRAY['最新技術を搭載した高品質な製品です。', '日常使いに最適な電子機器です。', '高い耐久性とパフォーマンスを実現しました。', 'コンパクトで持ち運びに便利です。', 'ビジネスにもプライベートにも活躍します。'];
    food_desc      TEXT[] := ARRAY['厳選された素材を使用した自信の一品です。', '毎日の食卓を豊かにする美味しさです。', '贈り物にも最適な逸品です。', '安心安全な国内製造品です。', '素材の味を大切にした商品です。'];
    cloth_desc     TEXT[] := ARRAY['着心地の良い素材を使用しています。', 'シーズンを問わず着用できます。', 'トレンドを取り入れたデザインです。', '洗濯機で洗えるイージーケア素材です。', 'オフィスからカジュアルまで幅広く対応します。'];
    book_desc      TEXT[] := ARRAY['初心者から上級者まで幅広くカバーしています。', '豊富な図解でわかりやすく解説します。', '実践的な演習問題付きです。', '最新の情報を盛り込んだ改訂版です。', 'ベストセラー著者による待望の新刊です。'];
    daily_desc     TEXT[] := ARRAY['毎日の暮らしを快適にする定番商品です。', '環境にやさしい素材を使用しています。', '大容量でコストパフォーマンスに優れています。', '肌にやさしい成分を配合しています。', '使いやすさを追求した設計です。'];

    -- 生成用変数
    v_product_code VARCHAR(20);
    v_product_name VARCHAR(200);
    v_price        NUMERIC(12,2);
    v_stock        INTEGER;
    v_status       VARCHAR(20);
    v_description  TEXT;
    v_created_at   TIMESTAMP;
    v_updated_at   TIMESTAMP;

    -- 価格レンジ
    v_price_min    INTEGER;
    v_price_max    INTEGER;

    -- 疑似乱数シード用
    v_seed         INTEGER;

    -- 配列インデックス
    p_idx          INTEGER;
    n_idx          INTEGER;
    s_idx          INTEGER;
    d_idx          INTEGER;
    status_rand    INTEGER;
BEGIN
    -- カテゴリごとに600件生成
    FOR cat_idx IN 1..5 LOOP
        v_category := categories[cat_idx];

        -- 価格レンジ設定
        CASE cat_idx
            WHEN 1 THEN v_price_min := 1000;  v_price_max := 100000;  -- 電子機器
            WHEN 2 THEN v_price_min := 100;   v_price_max := 5000;    -- 食品
            WHEN 3 THEN v_price_min := 500;   v_price_max := 30000;   -- 衣類
            WHEN 4 THEN v_price_min := 300;   v_price_max := 10000;   -- 書籍
            WHEN 5 THEN v_price_min := 100;   v_price_max := 8000;    -- 日用品
        END CASE;

        FOR item_in_cat IN 1..600 LOOP
            rec_num := (cat_idx - 1) * 600 + item_in_cat;

            -- 商品コード: PRD-0001 ~ PRD-3000
            v_product_code := 'PRD-' || LPAD(rec_num::TEXT, 4, '0');

            -- 疑似乱数シード（決定的だが散らばる値）
            v_seed := (rec_num * 7 + cat_idx * 13 + item_in_cat * 31) % 10000;

            -- 配列インデックス（1始まり）
            p_idx := (item_in_cat % 10) + 1;        -- prefix: 1..10
            n_idx := ((item_in_cat / 10) % 12) + 1; -- noun:   1..12
            s_idx := ((v_seed) % 10) + 1;            -- suffix: 1..10
            d_idx := ((item_in_cat + cat_idx) % 5) + 1; -- desc: 1..5

            -- 商品名生成
            CASE cat_idx
                WHEN 1 THEN
                    v_product_name := elec_prefix[p_idx] || elec_noun[n_idx] || elec_suffix[s_idx];
                    v_description  := elec_desc[d_idx];
                WHEN 2 THEN
                    v_product_name := food_prefix[p_idx] || food_noun[n_idx] || food_suffix[s_idx];
                    v_description  := food_desc[d_idx];
                WHEN 3 THEN
                    v_product_name := cloth_prefix[p_idx] || cloth_noun[n_idx] || cloth_suffix[s_idx];
                    v_description  := cloth_desc[d_idx];
                WHEN 4 THEN
                    v_product_name := book_prefix[p_idx] || book_noun[n_idx] || book_suffix[s_idx];
                    v_description  := book_desc[d_idx];
                WHEN 5 THEN
                    v_product_name := daily_prefix[p_idx] || daily_noun[n_idx] || daily_suffix[s_idx];
                    v_description  := daily_desc[d_idx];
            END CASE;

            -- 価格（疑似ランダム、カテゴリの範囲内）
            v_price := v_price_min + ((v_seed * 37 + item_in_cat * 53) % (v_price_max - v_price_min + 1));

            -- 在庫数 (0-500)
            v_stock := (v_seed * 23 + item_in_cat * 11) % 501;

            -- ステータス: ACTIVE ~80%, INACTIVE ~15%, DISCONTINUED ~5%
            status_rand := (v_seed * 41 + item_in_cat * 17) % 100;
            IF status_rand < 80 THEN
                v_status := 'ACTIVE';
            ELSIF status_rand < 95 THEN
                v_status := 'INACTIVE';
            ELSE
                v_status := 'DISCONTINUED';
            END IF;

            -- タイムスタンプ: 2024-01-01 ~ 2025-12-31 の範囲
            v_created_at := '2024-01-01 00:00:00'::TIMESTAMP
                + ((v_seed * 53 + item_in_cat * 7) % 730) * INTERVAL '1 day'
                + ((v_seed * 19 + item_in_cat * 3) % 1440) * INTERVAL '1 minute';

            -- updated_at は created_at と同日 ~ +90日
            v_updated_at := v_created_at
                + ((v_seed * 11 + item_in_cat * 29) % 91) * INTERVAL '1 day'
                + ((v_seed * 43 + item_in_cat * 13) % 1440) * INTERVAL '1 minute';

            INSERT INTO products (
                product_code, product_name, category, price,
                stock_quantity, status, description,
                created_at, updated_at
            ) VALUES (
                v_product_code, v_product_name, v_category, v_price,
                v_stock, v_status, v_description,
                v_created_at, v_updated_at
            );

        END LOOP;
    END LOOP;

    RAISE NOTICE 'サンプルデータ % 件を挿入しました。', rec_num;
END $$;
