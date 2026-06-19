content = open(r'D:\Billing App\app\src\main\java\com\shop\billing\ui\screens\history\HistoryScreen.kt', 'r', encoding='utf-8', errors='replace')
depth = 0
for i, line in enumerate(content.readlines(), 1):
    line = line.rstrip()
    if not line:
        continue
    in_string = False
    escapes = 0
    chars = list(line)
    clean = []
    for c in chars:
        if c == '"' and not escapes % 2:
            in_string = not in_string
        if not in_string:
            clean.append(c)
        if c == '\\':
            escapes += 1
        else:
            escapes = 0
    clean_line = ''.join(clean)

    opens = clean_line.count('{')
    closes = clean_line.count('}')
    delta = opens - closes
    depth += delta
    if depth < 0:
        print(f'Line {i}: UNDERFLOW depth={depth} | {line[:60]}')
    elif depth == 0 and i > 80 and i < 495:
        print(f'Line {i}: Depth=0 | {line[:60]}')
print(f'Final depth: {depth}')