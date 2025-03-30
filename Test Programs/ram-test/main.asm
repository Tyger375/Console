; Testing RAM
; Work RAM is in area 0x4000-0x5FFF
    org 0
main:
    nop
    nop
    xor a
    ld hl, $4000
    ld de, $F000
    ld (hl), a
loop:
    inc a
    ld b, a
    ld a, (hl)
    ld (de), a
    ld a, b
    ld (hl), a
    jr loop