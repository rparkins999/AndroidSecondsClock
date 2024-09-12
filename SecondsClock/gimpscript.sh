#!/usr/bin/bash
gimp -i -b - <<end
; Create object 1 as a new image
(gimp-image-new 144 144 0)
; White on black, not black on white
(gimp-context-swap-colors)
; Create object 2 as a text layer in that image
(gimp-text-layer-new 1 "$1\n$2\n$3\n$4\n$5" "arial" 25 0)
; Add the layer to the image
(gimp-image-insert-layer 1 2 0 0)
; Centre the text
(gimp-text-layer-set-justification 2 2)
(define x (quotient (- (car (gimp-image-width 1)) (car (gimp-drawable-width 2))) 2))
(gimp-layer-set-offsets 2 x 0)
; Force black backgground
(gimp-layer-flatten 2)
; Froce width to fit image width
(gimp-layer-resize-to-image-size 2)
; Export the icon
(file-png-save 1 1 2 "src/main/res/drawable/ic_launcher.png" "src/main/res/drawable/ic_launcher.png" 0 0 0 0 0 0 0)
; Exit
(gimp-quit TRUE)
end
