from PIL import Image

def measure_neck(image_path):
    img = Image.open(image_path).convert("RGBA")
    width, height = img.size
    pixels = img.load()
    
    # Identify background color (assume top-left is bg)
    bg_color = pixels[0, 0]
    
    # Scan bottom row
    y = height - 1
    start_x = -1
    end_x = -1
    
    for x in range(width):
        # Check if pixel is NOT background
        pixel = pixels[x, y]
        is_bg = sum(abs(c1 - c2) for c1, c2 in zip(pixel[:3], bg_color[:3])) < 50
        
        if not is_bg:
            if start_x == -1:
                start_x = x
            end_x = x
            
    if start_x != -1:
        neck_width = end_x - start_x + 1
        print(f"Neck Base Width: {neck_width} (from x={start_x} to {end_x})")
        print(f"Total Image Width: {width}")
        return neck_width, width
    else:
        print("Could not detect neck at bottom row.")
        return 0, width

measure_neck("c:/Users/Will/Antigravity/Kotlin-test/src/main/resources/assets/giraffe_head.png")
