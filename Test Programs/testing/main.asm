; Playing around with Z80 asm
    org 0
main:
    ld a, (_string_size)
    ld b, a
    ld hl, _string
loop_string:
    ld a, (hl)
    ld ($F000), a
    inc hl
    dec b
    jr nz, loop_string
    jr main

_string::
    .asciz "Hello World!"
_string_size::
    .db 13