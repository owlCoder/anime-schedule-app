from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


OUT = Path(__file__).parent

VIOLET_START = (139, 92, 246)   # #8B5CF6
VIOLET_END = (91, 33, 182)      # #5B21B6


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    family = "segoeuib.ttf" if bold else "segoeui.ttf"
    return ImageFont.truetype(f"C:/Windows/Fonts/{family}", size=size)


def gradient(size: tuple[int, int], start: tuple[int, int, int], end: tuple[int, int, int]) -> Image.Image:
    width, height = size
    image = Image.new("RGB", size)
    pixels = image.load()
    denominator = max(width + height - 2, 1)
    for y in range(height):
        for x in range(width):
            t = (x + y) / denominator
            pixels[x, y] = tuple(round(a + (b - a) * t) for a, b in zip(start, end))
    return image


def draw_monogram(draw: ImageDraw.ImageDraw, center: tuple[int, int], scale: float, color: str = "#FFFFFF") -> None:
    """'AS' monogram: a bold triangular 'A' (hollow crossbar) + a compact 'S' raised to
    its upper-right — same glyph as ic_launcher_foreground.xml, redrawn at asset scale."""
    cx, cy = center

    def pt(x: float, y: float) -> tuple[float, float]:
        # Source glyph is authored on a 0..108 viewport, origin centred at (54, 54).
        return (cx + (x - 54) * scale, cy + (y - 54) * scale)

    a_outer = [
        pt(40, 30), pt(58, 30), pt(72, 78), pt(61, 78),
        pt(57.5, 66), pt(40.5, 66), pt(37, 78), pt(26, 78),
    ]
    draw.polygon(a_outer, fill=color)
    a_hole = [pt(43.7, 56), pt(54.3, 56), pt(49, 37.5)]
    # Punch the crossbar hole using the background gradient colour sampled at centre.
    hole_fill = draw._image.getpixel((int(cx), int(cy))) if hasattr(draw, "_image") else (91, 33, 182)
    draw.polygon(a_hole, fill=hole_fill)

    s_path = [
        pt(62, 32), pt(62, 28.5), pt(65.2, 26), pt(70.2, 26), pt(73.9, 26), pt(76.7, 27.4), pt(78.6, 29.6),
        pt(74.8, 33.4), pt(73.6, 32.1), pt(71.9, 31.1), pt(70.1, 31.1), pt(68.1, 31.1), pt(66.9, 32),
        pt(66.9, 33.2), pt(66.9, 34.6), pt(68.3, 35.1), pt(71.2, 35.9), pt(75.6, 37), pt(78.8, 38.6),
        pt(78.8, 43), pt(78.8, 47), pt(75.4, 49.9), pt(69.2, 49.9), pt(64.7, 49.9), pt(61.3, 48.3), pt(59, 45.7),
        pt(63.2, 41.9), pt(64.7, 43.6), pt(66.7, 44.7), pt(69.2, 44.7), pt(71.6, 44.7), pt(73, 43.8), pt(73, 42.4),
        pt(73, 41), pt(71.7, 40.4), pt(68, 39.6), pt(63.3, 38.5), pt(60, 36.7), pt(60, 32),
    ]
    draw.polygon(s_path, fill=color)


def create_icon() -> None:
    icon = gradient((512, 512), VIOLET_START, VIOLET_END)

    glow = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse((-120, -140, 450, 430), fill=(255, 255, 255, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(95))
    icon = Image.alpha_composite(icon.convert("RGBA"), glow)

    shadow = Image.new("RGBA", icon.size, (0, 0, 0, 0))
    draw_monogram(ImageDraw.Draw(shadow), (263, 269), 5.2, "#40000000")
    shadow = shadow.filter(ImageFilter.GaussianBlur(5))
    icon = Image.alpha_composite(icon, shadow)
    draw_monogram(ImageDraw.Draw(icon), (256, 256), 5.2)
    icon.convert("RGB").save(OUT / "animeschedule-icon-512.png", optimize=True)


def create_feature_graphic() -> None:
    image = gradient((1024, 500), (16, 12, 28), (58, 24, 92)).convert("RGBA")

    glow = Image.new("RGBA", image.size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.ellipse((620, -270, 1210, 320), fill=(139, 92, 246, 95))
    glow_draw.ellipse((-220, 280, 350, 700), fill=(139, 92, 246, 35))
    image = Image.alpha_composite(image, glow.filter(ImageFilter.GaussianBlur(95)))

    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((94, 126, 342, 374), radius=60, fill="#5B21B6")
    draw_monogram(draw, (218, 250), 3.2)

    draw.text((404, 148), "AnimeSchedule", font=font(64, bold=True), fill="#FFFFFF")
    draw.text(
        (408, 240),
        "Track anime airing schedules,\nall in one place.",
        font=font(31),
        fill="#DCD3F0",
        spacing=10,
    )
    image.convert("RGB").save(OUT / "animeschedule-feature-1024x500.png", optimize=True)


if __name__ == "__main__":
    create_icon()
    create_feature_graphic()
