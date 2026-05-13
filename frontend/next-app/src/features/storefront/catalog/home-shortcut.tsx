import Link from "next/link";

type HomeShortcutProps = {
  label?: string;
};

export function HomeShortcut({ label = "返回首页" }: HomeShortcutProps) {
  return (
    <Link className="rounded-full border border-black/10 px-4 py-2 text-sm font-medium" href="/">
      {label}
    </Link>
  );
}
