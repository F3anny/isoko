/** Compress a product photo to a light JPEG data URL for catalogue upload. */
export function compressProductPhoto(file, { maxSize = 720, quality = 0.62 } = {}) {
  return new Promise((resolve, reject) => {
    if (!file || !file.type.startsWith("image/")) {
      reject(new Error("Choose a photo file."));
      return;
    }
    const img = new Image();
    const objectUrl = URL.createObjectURL(file);
    img.onload = () => {
      const scale = Math.min(1, maxSize / Math.max(img.width, img.height));
      const width = Math.max(1, Math.round(img.width * scale));
      const height = Math.max(1, Math.round(img.height * scale));
      const canvas = document.createElement("canvas");
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext("2d");
      ctx.fillStyle = "#f4fafd";
      ctx.fillRect(0, 0, width, height);
      ctx.filter = "brightness(1.08) saturate(0.9) contrast(0.94)";
      ctx.drawImage(img, 0, 0, width, height);
      ctx.filter = "none";
      const dataUrl = canvas.toDataURL("image/jpeg", quality);
      URL.revokeObjectURL(objectUrl);
      resolve(dataUrl);
    };
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      reject(new Error("Could not read that photo."));
    };
    img.src = objectUrl;
  });
}
