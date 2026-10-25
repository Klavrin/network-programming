import random

COUNT = 50_000_000
MIN_VALUE = -1_000_000
MAX_VALUE = 1_000_000

with open("numbers.txt", "w") as file:
    for _ in range(COUNT):
        file.write(f"{random.randint(MIN_VALUE, MAX_VALUE)}\n")

print("Generation complete!")
