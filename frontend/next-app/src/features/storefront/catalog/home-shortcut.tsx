import Link from "next/link";

type HomeShortcutProps = {
  label?: string;
};

export function HomeShortcut({ label = "返回首页" }: HomeShortcutProps) {
  return (
    <Link className="btn-ghost px-0 py-0 text-sm" href="/">
      {label}
    </Link>
  );
}
