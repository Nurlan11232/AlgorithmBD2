from __future__ import annotations
import sys
import argparse
import pyphen
import time
import tracemalloc
from typing import List, Tuple, Dict
sys.setrecursionlimit(2000)
def get_hyphen(lang: str):
    if lang == "mn_MN":
        return pyphen.Pyphen(filename='/usr/share/hyphen/hyph_mn_MN.dic')
    elif lang == "en_US":
        return pyphen.Pyphen(lang='en_US')
    else:
        return None
def measure_memory(func, *args):
    tracemalloc.start()
    start = time.perf_counter()
    result = func(*args)
    end = time.perf_counter()
    current, peak = tracemalloc.get_traced_memory()
    tracemalloc.stop()
    return result, (end - start), current / 1024, peak / 1024
class Word:
    def __init__(self, text: str, parts: List[str], idx: int):
        self.text = text
        self.parts = parts
        self.idx = idx

    def __repr__(self):
        return f"<{self.text}>"

def prepare_words(text: str, lang: str, use_hyphen: bool = True) -> List[Word]:
    raw_words = text.split()
    hy = get_hyphen(lang) if use_hyphen else None
    out: List[Word] = []
    for i, w in enumerate(raw_words):
        if hy is None:
            out.append(Word(w, [w], i))
        else:
            inserted = hy.inserted(w)
            parts = inserted.split('-')
            out.append(Word(w, parts, i))
    return out

def justify_line(words_str: List[str], width: int, last_line=False) -> str:
    if last_line or len(words_str) == 1:
        line = " ".join(words_str)
        return line + " " * (max(0, width - len(line)))
    total_chars = sum(len(w) for w in words_str)
    spaces_needed = width - total_chars
    gaps = len(words_str) - 1
    if gaps == 0:
        return words_str[0] + " " * (width - len(words_str[0]))
    base = spaces_needed // gaps
    extra = spaces_needed % gaps
    out = []
    for i, w in enumerate(words_str):
        out.append(w)
        if i < gaps:
            out.append(" " * (base + (1 if i < extra else 0)))
    return "".join(out)

# --- Greedy алгоритм ---
def greedy(words_in: List[Word], width: int) -> List[str]:
    words = [Word(w.text, w.parts.copy(), w.idx) for w in words_in]
    lines = []
    i = 0
    n = len(words)
    while i < n:
        cur = []
        cur_len = 0
        while i < n:
            w = words[i]
            need = len(w.text) + (1 if cur else 0)
            if cur_len + need <= width:
                cur.append(w.text)
                cur_len += need
                i += 1
            else:
                break
        if i < n:
            w = words[i]
            best_break = None
            for p in range(len(w.parts) - 1):
                prefix_text = "".join(w.parts[:p + 1])
                prefix_with_hyphen = prefix_text + "-"
                need = len(prefix_with_hyphen) + (1 if cur else 0)
                if cur_len + need <= width:
                    best_break = p
            if best_break is not None:
                prefix = "".join(w.parts[:best_break + 1]) + "-"
                cur.append(prefix)
                remaining_text = "".join(w.parts[best_break + 1:])
                remaining_parts = w.parts[best_break + 1:]
                words[i] = Word(remaining_text, remaining_parts, w.idx)
            elif not cur:
                cur.append(w.text)
                i += 1
        last = (i >= n)
        lines.append(justify_line(cur, width, last_line=last))
    return lines

def dp_iterative(words: List[Word], width: int) -> List[str]:
    N = len(words)
    dp_cost: Dict[Tuple[int, int], float] = {}
    dp_path: Dict[Tuple[int, int], Tuple[int, int, List[str]]] = {}
    dp_cost[(N, 0)] = 0

    for i in range(N - 1, -1, -1):
        num_parts = len(words[i].parts)
        for p in range(num_parts - 1, -1, -1):
            current_state = (i, p)
            best_c = float('inf')
            best_next_state = None
            best_line_content = None

            remaining_parts = words[i].parts[p:]
            if len(remaining_parts) > 1:
                for k in range(len(remaining_parts) - 1):
                    chunk_parts = remaining_parts[:k + 1]
                    chunk_text = "".join(chunk_parts) + "-"
                    if len(chunk_text) <= width:
                        split_cost = (width - len(chunk_text)) ** 2
                        next_p = p + (k + 1)
                        next_state = (i, next_p)
                        if next_state in dp_cost:
                            total = split_cost + dp_cost[next_state]
                            if total < best_c:
                                best_c = total
                                best_next_state = next_state
                                best_line_content = [chunk_text]

            suffix_text = "".join(remaining_parts)
            current_line = [suffix_text]
            current_len = len(suffix_text)

            if current_len <= width:
                next_state = (i + 1, 0)
                is_last_word = (i == N - 1)
                line_cost = 0 if is_last_word else (width - current_len) ** 2
                if next_state in dp_cost:
                    total = line_cost + dp_cost[next_state]
                    if total < best_c:
                        best_c = total
                        best_next_state = next_state
                        best_line_content = list(current_line)

                for j in range(i + 1, N):
                    next_w = words[j]
                    new_len = current_len + 1 + len(next_w.text)
                    if new_len <= width:
                        current_line.append(next_w.text)
                        current_len = new_len
                        next_state = (j + 1, 0)
                        is_last_word = (j == N - 1)
                        line_cost = 0 if is_last_word else (width - current_len) ** 2
                        if next_state in dp_cost:
                            total = line_cost + dp_cost[next_state]
                            if total < best_c:
                                best_c = total
                                best_next_state = next_state
                                best_line_content = list(current_line)
                    else:
                        if len(next_w.parts) > 1:
                            for k in range(len(next_w.parts) - 1):
                                prefix = "".join(next_w.parts[:k + 1]) + "-"
                                split_len = current_len + 1 + len(prefix)
                                if split_len <= width:
                                    split_cost = (width - split_len) ** 2
                                    next_state = (j, k + 1)
                                    if next_state in dp_cost:
                                        total = split_cost + dp_cost[next_state]
                                        if total < best_c:
                                            best_c = total
                                            best_next_state = next_state
                                            best_line_content = current_line + [prefix]
                        break

            if best_c == float('inf'):
                best_c = 1000000
                best_next_state = (i + 1, 0)
                best_line_content = [suffix_text]

            dp_cost[current_state] = best_c
            dp_path[current_state] = (best_next_state, best_line_content)

    lines = []
    curr = (0, 0)
    while curr != (N, 0):
        if curr not in dp_path:
            break
        next_s, content = dp_path[curr]
        is_last = (next_s == (N, 0))
        formatted = justify_line(content, width, last_line=is_last)
        lines.append(formatted)
        curr = next_s
    return lines

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("input", help="input text file")
    parser.add_argument("width", type=int)
    parser.add_argument("--lang", default="mn_MN")
    parser.add_argument("--hyphen", action="store_true")
    args = parser.parse_args()

    with open(args.input, "r", encoding="utf-8") as f:
        text = f.read()

    words = prepare_words(text, args.lang, use_hyphen=args.hyphen)

    print("GREEDY ")
    words_g = [Word(w.text, w.parts.copy(), w.idx) for w in words]
    (lines_g, t_g, mem_g_cur, mem_g_peak) = measure_memory(greedy, words_g, args.width)
    for ln in lines_g:
        print("|" + ln + "|")

    print("DP :")
    (lines_dp, t_dp, mem_dp_cur, mem_dp_peak) = measure_memory(dp_iterative, words, args.width)
    for ln in lines_dp:
        print("|" + ln + "|")

    print("Algorithmiin haritsuulalt")
    print(f"Line width: {args.width}")
    print(f"Total words: {len(words)}\n")
    print("GREEDY RESULT:")
    print(f" - Time: {t_g*1000:.3f} ms")
    print(f" - Memory (peak): {mem_g_peak:.2f} KB\n")
    print("DP RESULT:")
    print(f" - Time: {t_dp*1000:.3f} ms")
    print(f" - Memory (peak): {mem_dp_peak:.2f} KB\n")
    speed_winner = "GREEDY" if t_g < t_dp else "DP"
    memory_winner = "GREEDY" if mem_g_peak < mem_dp_peak else "DP"
    print("FINAL RESULT:")
    print(f"  Speed winner: {speed_winner}")
    print(f"  Memory winner: {memory_winner}")
if __name__ == "__main__":
    main()
