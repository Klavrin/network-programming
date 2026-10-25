negative = 0
positive = 0
zero = 0

with open("numbers.txt") as file:
    for line in file:
        number = int(line)

        if number < 0:
            negative += 1
        elif number > 0:
            positive += 1
        else:
            zero += 1

print(f"Negative: {negative}")
print(f"Positive: {positive}")
print(f"Zero:     {zero}")
print(f"Total:    {negative + positive + zero}")
