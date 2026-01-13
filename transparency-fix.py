from PIL import Image
import os

def process_image(file_path, target_color, replacement_color, tolerance=50):
    try:
        print(f"Processing {file_path}...")
        img = Image.open(file_path).convert("RGBA")
        width, height = img.size
        pixels = img.load()
        changed_count = 0

        for y in range(height):
            for x in range(width):
                current_color = pixels[x, y]
                
                # Calculate difference manually
                diff = sum(abs(c1 - c2) for c1, c2 in zip(current_color[:3], target_color[:3]))
                
                if diff <= tolerance:
                    pixels[x, y] = replacement_color
                    changed_count += 1
        
        img.save(file_path, "PNG")
        print(f"  -> Changed {changed_count} pixels in {file_path}")
    except Exception as e:
        print(f"  -> Failed: {e}")

if __name__ == "__main__":
    assets_dir = "c:/Users/Will/Antigravity/Kotlin-test/src/main/resources/assets"
    
    # 1. Ground: Magenta -> Black (Solid Opaque)
    # Magenta is roughly (255, 0, 255)
    # User requested Black which is (0, 0, 0, 255)
    process_image(
        os.path.join(assets_dir, "ground.png"), 
        target_color=(255, 0, 255), 
        replacement_color=(0, 0, 0, 255),
        tolerance=80
    )

    # 2. Giraffe Head: Green -> Transparent
    # Green is roughly (0, 255, 0)
    process_image(
        os.path.join(assets_dir, "giraffe_head.png"), 
        target_color=(0, 255, 0), 
        replacement_color=(0, 0, 0, 0),
        tolerance=80
    )
