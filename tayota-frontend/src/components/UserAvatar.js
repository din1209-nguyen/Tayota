"use client";

const DEFAULT_AVATAR_PATHS = new Set(["/default-avatar.png"]);

export default function UserAvatar({ className = "", label = "Anh dai dien", src = "" }) {
  const rawSrc = typeof src === "string" ? src.trim() : "";
  const avatarSrc = DEFAULT_AVATAR_PATHS.has(rawSrc) ? "" : rawSrc;
  const classes = ["user-avatar", avatarSrc ? "has-image" : "is-default", className].filter(Boolean).join(" ");

  return (
    <span
      aria-label={label}
      className={classes}
      role="img"
      style={avatarSrc ? { backgroundImage: `url(${avatarSrc})` } : undefined}
    >
      {avatarSrc ? null : (
        <svg className="user-avatar-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
          <path d="M12 11.72c2.5 0 4.46-2.07 4.46-4.72S14.5 2.28 12 2.28 7.54 4.35 7.54 7 9.5 11.72 12 11.72Z" />
          <path d="M3.38 22.1c.54-5.08 4.02-8.25 8.62-8.25s8.08 3.17 8.62 8.25H3.38Z" />
        </svg>
      )}
    </span>
  );
}
