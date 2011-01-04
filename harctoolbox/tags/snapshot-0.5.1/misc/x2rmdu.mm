!declaration

!declare *index := 0;

!transformation

!begin []
source.child
;

Function []
"Function." || *index || ".name=" || self.@name || "\n",
"Function." || *index || ".hex=" || self.@hex || "\n",
(*index := *index + 1).null
;

Button [^@shifted & ^@unshifted &^@xshifted]
!replace
;
[]
"Button." || self.@number || "=",
self.r.@unshifted | "null", "|",
self.r.@shifted | "null", "|",
self.r.@xshifted | "null",
"\n"
;

Code []
!replace
"Code." || self.@cpu || "=" || self.r.data || "\n"
;

!default []
!replace
self.gid || "=" || self.r.data || "\n"
;
