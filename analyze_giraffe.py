from PIL import Image
import os

base_path = "c:/Users/Will/Antigravity/Kotlin-test/src/main/resources/assets/"
files = ["giraffe_long_0.png", "giraffe_long_1.png", "giraffe_long_2.png", "giraffe_long_3.png"]

max_height = 0
best_ratio = 0
best_file = ""

print(f"{'File':<25} | {'Size (WxH)':<15} | {'BBox (WxH)':<15} | {'Aspect Ratio':<15}")
print("-" * 80)

for f in files:
    path = os.path.join(base_path, f)
    try:
        img = Image.open(path).convert("RGBA")
        width, height = img.size
        bbox = img.getbbox() # Returns (left, upper, right, lower)
        
        if bbox:
            bbox_width = bbox[2] - bbox[0]
            bbox_height = bbox[3] - bbox[1]
            ratio = bbox_width / bbox_height
            
            print(f"{f:<25} | {width}x{height:<12} | {bbox_width}x{bbox_height:<10} | {ratio:.4f}")
            
            if bbox_height > max_height:
                max_height = bbox_height
                best_ratio = ratio
                best_file = f
        else:
             print(f"{f:<25} | {width}x{height:<12} | {'Empty':<14} | N/A")

    except Exception as e:
        print(f"Error processing {f}: {e}")

print("-" * 80)
print(f"Longest (tallest) content found in: {best_file}")
print(f"Aspect Ratio of content: {best_ratio:.5f}")
