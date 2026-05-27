ALTER TABLE "CAR_VERSION" ADD COLUMN IF NOT EXISTS is_visible BOOLEAN DEFAULT TRUE;
UPDATE "CAR_VERSION" SET is_visible = TRUE WHERE is_visible IS NULL;
ALTER TABLE "CAR_VERSION" ALTER COLUMN is_visible SET DEFAULT TRUE;
ALTER TABLE "CAR_VERSION" ALTER COLUMN is_visible SET NOT NULL;

ALTER TABLE "CAR_VERSION" ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);
UPDATE "CAR_VERSION" cv
SET image_url = COALESCE(
    NULLIF(cv.image_url, ''),
    (
        SELECT cp.ex_image_url
        FROM "CAR_PRICE" cp
        WHERE cp.car_version_id = cv.id
          AND cp.ex_image_url IS NOT NULL
          AND cp.ex_image_url <> ''
        LIMIT 1
    ),
    (
        SELECT cg.image_url
        FROM "CAR_GALLERY" cg
        WHERE cg.car_version_id = cv.id
          AND cg.image_url IS NOT NULL
          AND cg.image_url <> ''
        LIMIT 1
    )
)
WHERE cv.image_url IS NULL OR cv.image_url = '';

ALTER TABLE "ACCESSORY" ADD COLUMN IF NOT EXISTS is_visible BOOLEAN DEFAULT TRUE;
UPDATE "ACCESSORY" SET is_visible = TRUE WHERE is_visible IS NULL;
ALTER TABLE "ACCESSORY" ALTER COLUMN is_visible SET DEFAULT TRUE;
ALTER TABLE "ACCESSORY" ALTER COLUMN is_visible SET NOT NULL;

ALTER TABLE "SERVICE" DROP CONSTRAINT IF EXISTS "SERVICE_status_check";
ALTER TABLE "SERVICE" ADD CONSTRAINT "SERVICE_status_check"
    CHECK (status IN ('CONFIRMED', 'NEEDS_REASSIGNMENT', 'RECEIVING', 'IN_PROGRESS', 'COMPLETED', 'CANCELED', 'EXPIRED'));
