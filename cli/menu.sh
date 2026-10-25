# Arrow-key menu for bash (works with macOS's bash 3.2). Sourced by lab.sh; knows nothing about the lab.
#
#   menu_select "Title" "Option 1" "Option 2" ...   -> MENU_CHOICE = index of the chosen option, or -1 on q / Esc
#                                                      (MENU_START, if set, is the option highlighted first)
#   pause                                            -> waits for any key

bold=$'\033[1m'; dim=$'\033[2m'; invert=$'\033[7m'; reset=$'\033[0m'

hide_cursor()  { printf '\033[?25l'; }
show_cursor()  { printf '\033[?25h'; }
clear_screen() { printf '\033[H\033[2J'; }

trap show_cursor EXIT

# Reads one key and prints its name: up, down, enter, quit or other.
read_key() {
    local key rest
    IFS= read -rsn1 key || { echo quit; return; }
    if [[ "$key" == $'\033' ]]; then
        IFS= read -rsn2 -t 1 rest || rest=""
        key+="$rest"
    fi
    case "$key" in
        $'\033[A' | k) echo up ;;
        $'\033[B' | j) echo down ;;
        "")            echo enter ;;
        q | Q | $'\033') echo quit ;;
        *)             echo other ;;
    esac
}

menu_select() {
    local title="$1"; shift
    local options=("$@") count=$# selected=${MENU_START:-0} i

    hide_cursor
    while true; do
        clear_screen
        printf '\n  %s%s%s\n\n' "$bold" "$title" "$reset"
        for i in "${!options[@]}"; do
            if [[ $i -eq $selected ]]; then
                printf '  %s ❯ %s %s\n' "$invert" "${options[$i]}" "$reset"
            else
                printf '     %s\n' "${options[$i]}"
            fi
        done
        printf '\n  %s↑/↓ move   enter select   q back%s\n' "$dim" "$reset"

        case "$(read_key)" in
            up)    selected=$(( (selected - 1 + count) % count )) ;;
            down)  selected=$(( (selected + 1) % count )) ;;
            enter) MENU_CHOICE=$selected; break ;;
            quit)  MENU_CHOICE=-1; break ;;
        esac
    done
    show_cursor
    clear_screen
}

pause() {
    printf '\n  %sPress any key to return to the menu…%s' "$dim" "$reset"
    IFS= read -rsn1 _ || true
}
