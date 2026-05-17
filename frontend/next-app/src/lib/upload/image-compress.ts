const SUPPORTED_IMAGE_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);

type CompressOptions = {
  maxWidth?: number;
  quality?: number;
  maxSize?: number;
};

export async function compressImage(file: File, options: CompressOptions = {}): Promise<Blob> {
  const maxWidth = options.maxWidth ?? 1920;
  const quality = options.quality ?? 0.8;
  const maxSize = options.maxSize ?? 10;

  if (!SUPPORTED_IMAGE_TYPES.has(file.type)) {
    throw new Error("不支持的图片格式，仅支持 jpg / png / webp");
  }

  if (file.size > maxSize * 1024 * 1024) {
    throw new Error(`文件大小超过限制，最大 ${maxSize}MB`);
  }

  const image = await loadImage(file);
  const scale = image.width > maxWidth ? maxWidth / image.width : 1;
  const width = Math.max(1, Math.round(image.width * scale));
  const height = Math.max(1, Math.round(image.height * scale));
  const canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;

  const context = canvas.getContext("2d");
  if (!context) {
    throw new Error("当前浏览器不支持图片压缩");
  }

  context.drawImage(image, 0, 0, width, height);

  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          reject(new Error("图片压缩失败"));
          return;
        }
        resolve(blob);
      },
      file.type,
      quality
    );
  });
}

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const objectUrl = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(objectUrl);
      resolve(image);
    };
    image.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("图片读取失败"));
    };
    image.src = objectUrl;
  });
}
