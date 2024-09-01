import { RenderableProps } from "preact"
import * as React from "preact/compat"

type IconProps = RenderableProps<{ maxHeight: number }>

export const A4Logo = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} xmlns="http://www.w3.org/2000/svg" viewBox="0 0 135.46666 135.46667">
    <ellipse cx="67.73333" cy="67.493835" rx="59.259792" ry="60.046299" style="fill:none;fill-opacity:1;stroke:#5d6b86;stroke-width:6.34999992;stroke-dasharray:none;stroke-dashoffset:0;stroke-opacity:1"/>
    <path d="M19.84375 33.866666C31.823897 16.239972 50.800009 8.4858568 67.733331 8.4666664 84.620124 8.4475287 102.72538 15.70563 115.62292 33.866666L67.733331 67.493833Z" style="fill:#364c53;fill-opacity:1;stroke:none;stroke-width:2.10817;stroke-dasharray:none;stroke-opacity:1"/>
    <path d="M101.64496 115.4286c-20.178729 15.6864-51.000221 14.76276-71.547226-2.93004l37.635597-42.648562Z" style="fill:#4a0c0c;fill-opacity:1;stroke:none;stroke-width:2.11667;stroke-dasharray:none;stroke-opacity:1"/>
    <path d="m67.733331 67.493833 42.876229-39.873298c1.60038 1.748374 3.09896 3.625184 4.53503 5.580851zM67.733331 67.493833 24.776133 27.570907c-1.664113 1.782062-3.03566 3.687083-4.437769 5.57926z" style="fill:#2f4249;fill-opacity:1;stroke:none;stroke-width:2.11667;stroke-dasharray:none;stroke-opacity:1"/>
    <path d="M67.733331 69.849998 101.64496 115.4286c-1.765678 1.36632-2.304757 1.68833-4.605782 3.18481zM67.733331 69.849998 30.097734 112.49856c1.94302 1.63401 3.773333 2.86579 5.958049 4.48538z" style="fill:#521010;fill-opacity:1;stroke:none;stroke-width:2.11667;stroke-dasharray:none;stroke-opacity:1"/>
  </svg>
)

export const A4IcnPlay = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M21.4086 9.35258C23.5305 10.5065 23.5305 13.4935 21.4086 14.6474L8.59662 21.6145C6.53435 22.736 4 21.2763 4 18.9671L4 5.0329C4 2.72368 6.53435 1.26402 8.59661 2.38548L21.4086 9.35258Z" fill="#88c0d0"/>
  </svg>  
)

export const A4IcnStop = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M2 12C2 7.28595 2 4.92893 3.46447 3.46447C4.92893 2 7.28595 2 12 2C16.714 2 19.0711 2 20.5355 3.46447C22 4.92893 22 7.28595 22 12C22 16.714 22 19.0711 20.5355 20.5355C19.0711 22 16.714 22 12 22C7.28595 22 4.92893 22 3.46447 20.5355C2 19.0711 2 16.714 2 12Z" fill="#bf616a"/>
  </svg>
)

export const A4IconEdit = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M16.5189 16.5013C16.6939 16.3648 16.8526 16.2061 17.1701 15.8886L21.1275 11.9312C21.2231 11.8356 21.1793 11.6708 21.0515 11.6264C20.5844 11.4644 19.9767 11.1601 19.4083 10.5917C18.8399 10.0233 18.5356 9.41561 18.3736 8.94849C18.3292 8.82066 18.1644 8.77687 18.0688 8.87254L14.1114 12.8299C13.7939 13.1474 13.6352 13.3061 13.4987 13.4811C13.3377 13.6876 13.1996 13.9109 13.087 14.1473C12.9915 14.3476 12.9205 14.5606 12.7786 14.9865L12.5951 15.5368L12.3034 16.4118L12.0299 17.2323C11.9601 17.4419 12.0146 17.6729 12.1708 17.8292C12.3271 17.9854 12.5581 18.0399 12.7677 17.9701L13.5882 17.6966L14.4632 17.4049L15.0135 17.2214L15.0136 17.2214C15.4394 17.0795 15.6524 17.0085 15.8527 16.913C16.0891 16.8004 16.3124 16.6623 16.5189 16.5013Z" fill="#88c0d0"/>
    <path d="M22.3665 10.6922C23.2112 9.84754 23.2112 8.47812 22.3665 7.63348C21.5219 6.78884 20.1525 6.78884 19.3078 7.63348L19.1806 7.76071C19.0578 7.88348 19.0022 8.05496 19.0329 8.22586C19.0522 8.33336 19.0879 8.49053 19.153 8.67807C19.2831 9.05314 19.5288 9.54549 19.9917 10.0083C20.4545 10.4712 20.9469 10.7169 21.3219 10.847C21.5095 10.9121 21.6666 10.9478 21.7741 10.9671C21.945 10.9978 22.1165 10.9422 22.2393 10.8194L22.3665 10.6922Z" fill="#81a1c1"/>
    <path fill-rule="evenodd" clip-rule="evenodd" d="M4.17157 3.17157C3 4.34315 3 6.22876 3 10V14C3 17.7712 3 19.6569 4.17157 20.8284C5.34315 22 7.22876 22 11 22H13C16.7712 22 18.6569 22 19.8284 20.8284C20.9812 19.6756 20.9997 17.8316 21 14.1801L18.1817 16.9984C17.9119 17.2683 17.691 17.4894 17.4415 17.6841C17.1491 17.9121 16.8328 18.1076 16.4981 18.2671C16.2124 18.4032 15.9159 18.502 15.5538 18.6225L13.2421 19.3931C12.4935 19.6426 11.6682 19.4478 11.1102 18.8898C10.5523 18.3318 10.3574 17.5065 10.607 16.7579L10.8805 15.9375L11.3556 14.5121L11.3775 14.4463C11.4981 14.0842 11.5968 13.7876 11.7329 13.5019C11.8924 13.1672 12.0879 12.8509 12.316 12.5586C12.5106 12.309 12.7317 12.0881 13.0017 11.8183L17.0081 7.81188L18.12 6.70004L18.2472 6.57282C18.9626 5.85741 19.9003 5.49981 20.838 5.5C20.6867 4.46945 20.3941 3.73727 19.8284 3.17157C18.6569 2 16.7712 2 13 2H11C7.22876 2 5.34315 2 4.17157 3.17157ZM7.25 9C7.25 8.58579 7.58579 8.25 8 8.25H14.5C14.9142 8.25 15.25 8.58579 15.25 9C15.25 9.41421 14.9142 9.75 14.5 9.75H8C7.58579 9.75 7.25 9.41421 7.25 9ZM7.25 13C7.25 12.5858 7.58579 12.25 8 12.25H10.5C10.9142 12.25 11.25 12.5858 11.25 13C11.25 13.4142 10.9142 13.75 10.5 13.75H8C7.58579 13.75 7.25 13.4142 7.25 13ZM7.25 17C7.25 16.5858 7.58579 16.25 8 16.25H9.5C9.91421 16.25 10.25 16.5858 10.25 17C10.25 17.4142 9.91421 17.75 9.5 17.75H8C7.58579 17.75 7.25 17.4142 7.25 17Z" fill="#8fbcbb"/>
  </svg>
)

export const A4IcnSave = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fill-rule="evenodd" clip-rule="evenodd" d="M20.5355 20.5355C22 19.0711 22 16.714 22 12C22 11.6585 22 11.4878 21.9848 11.3142C21.9142 10.5049 21.586 9.71257 21.0637 9.09034C20.9516 8.95687 20.828 8.83317 20.5806 8.58578L15.4142 3.41944C15.1668 3.17206 15.0431 3.04835 14.9097 2.93631C14.2874 2.414 13.4951 2.08581 12.6858 2.01515C12.5122 2 12.3415 2 12 2C7.28595 2 4.92893 2 3.46447 3.46447C2 4.92893 2 7.28595 2 12C2 16.714 2 19.0711 3.46447 20.5355C4.1485 21.2196 5.02727 21.5841 6.25 21.7784L6.25 20.948C6.24997 20.0495 6.24995 19.3003 6.32991 18.7055C6.41432 18.0777 6.59999 17.5109 7.05546 17.0555C7.51093 16.6 8.07773 16.4143 8.70552 16.3299C9.3003 16.2499 10.0495 16.25 10.948 16.25H13.052C13.9505 16.25 14.6997 16.2499 15.2945 16.3299C15.9223 16.4143 16.4891 16.6 16.9445 17.0555C17.4 17.5109 17.5857 18.0777 17.6701 18.7055C17.7501 19.3003 17.75 20.0495 17.75 20.948L17.75 21.7784C18.9727 21.5841 19.8515 21.2196 20.5355 20.5355ZM6.25 8C6.25 7.58579 6.58579 7.25 7 7.25H13C13.4142 7.25 13.75 7.58579 13.75 8C13.75 8.41421 13.4142 8.75 13 8.75H7C6.58579 8.75 6.25 8.41421 6.25 8Z" fill="#8fbcbb"/>
    <path d="M16.1835 18.9054C16.2484 19.3884 16.25 20.036 16.25 21V21.9313C15.0942 22 13.7004 22 12 22C10.2996 22 8.9058 22 7.75 21.9313V21C7.75 20.036 7.75159 19.3884 7.81654 18.9054C7.87858 18.4439 7.9858 18.2464 8.11612 18.1161C8.24643 17.9858 8.44393 17.8786 8.90539 17.8165C9.38843 17.7516 10.036 17.75 11 17.75H13C13.964 17.75 14.6116 17.7516 15.0946 17.8165C15.5561 17.8786 15.7536 17.9858 15.8839 18.1161C16.0142 18.2464 16.1214 18.4439 16.1835 18.9054Z" fill="#5e81ac"/>
  </svg>
)

export const A4IcnAdd = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fill-rule="evenodd" clip-rule="evenodd" d="M12 22C7.28595 22 4.92893 22 3.46447 20.5355C2 19.0711 2 16.714 2 12C2 7.28595 2 4.92893 3.46447 3.46447C4.92893 2 7.28595 2 12 2C16.714 2 19.0711 2 20.5355 3.46447C22 4.92893 22 7.28595 22 12C22 16.714 22 19.0711 20.5355 20.5355C19.0711 22 16.714 22 12 22ZM12 8.25C12.4142 8.25 12.75 8.58579 12.75 9V11.25H15C15.4142 11.25 15.75 11.5858 15.75 12C15.75 12.4142 15.4142 12.75 15 12.75H12.75L12.75 15C12.75 15.4142 12.4142 15.75 12 15.75C11.5858 15.75 11.25 15.4142 11.25 15V12.75H9C8.58579 12.75 8.25 12.4142 8.25 12C8.25 11.5858 8.58579 11.25 9 11.25H11.25L11.25 9C11.25 8.58579 11.5858 8.25 12 8.25Z" fill="#a3be8c"/>
  </svg>  
)

export const A4Inspect = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <rect width="28" height="28"/>
    <path fill-rule="evenodd" clip-rule="evenodd" d="M7.25007 2.38782C8.54878 2.0992 10.1243 2 12 2C13.8757 2 15.4512 2.0992 16.7499 2.38782C18.06 2.67897 19.1488 3.176 19.9864 4.01358C20.824 4.85116 21.321 5.94002 21.6122 7.25007C21.9008 8.54878 22 10.1243 22 12C22 13.8757 21.9008 15.4512 21.6122 16.7499C21.321 18.06 20.824 19.1488 19.9864 19.9864C19.1488 20.824 18.06 21.321 16.7499 21.6122C15.4512 21.9008 13.8757 22 12 22C10.1243 22 8.54878 21.9008 7.25007 21.6122C5.94002 21.321 4.85116 20.824 4.01358 19.9864C3.176 19.1488 2.67897 18.06 2.38782 16.7499C2.0992 15.4512 2 13.8757 2 12C2 10.1243 2.0992 8.54878 2.38782 7.25007C2.67897 5.94002 3.176 4.85116 4.01358 4.01358C4.85116 3.176 5.94002 2.67897 7.25007 2.38782ZM9 11.5C9 10.1193 10.1193 9 11.5 9C12.8807 9 14 10.1193 14 11.5C14 12.8807 12.8807 14 11.5 14C10.1193 14 9 12.8807 9 11.5ZM11.5 7C9.01472 7 7 9.01472 7 11.5C7 13.9853 9.01472 16 11.5 16C12.3805 16 13.202 15.7471 13.8957 15.31L15.2929 16.7071C15.6834 17.0976 16.3166 17.0976 16.7071 16.7071C17.0976 16.3166 17.0976 15.6834 16.7071 15.2929L15.31 13.8957C15.7471 13.202 16 12.3805 16 11.5C16 9.01472 13.9853 7 11.5 7Z" fill="#88c0d0"/>
  </svg>
)

export const A4Status = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fill-rule="evenodd" clip-rule="evenodd" d="M8.96173 19.3707C6.01943 16.9714 2 13.0079 2 9.26044C2 3.3495 7.50016 0.662637 12 5.49877C16.4998 0.662637 22 3.34931 22 9.2604C22 13.008 17.9806 16.9714 15.0383 19.3707C13.7063 20.4569 13.0403 21 12 21C10.9597 21 10.2937 20.4569 8.96173 19.3707ZM10.0932 10.7463C10.1827 10.6184 10.2571 10.5122 10.3233 10.4213C10.3793 10.5188 10.4418 10.6324 10.517 10.7692L12.2273 13.8787C12.3933 14.1808 12.5562 14.4771 12.7197 14.6921C12.8947 14.9221 13.2023 15.2374 13.6954 15.2466C14.1884 15.2559 14.5077 14.9524 14.6912 14.7291C14.8627 14.5204 15.0365 14.2305 15.2138 13.9349L15.2692 13.8425C15.49 13.4745 15.629 13.2444 15.752 13.0782C15.8654 12.9251 15.9309 12.8751 15.9798 12.8475C16.0286 12.8198 16.1052 12.7894 16.2948 12.7709C16.5006 12.7509 16.7694 12.7501 17.1986 12.7501H18C18.4142 12.7501 18.75 12.4143 18.75 12.0001C18.75 11.5859 18.4142 11.2501 18 11.2501L17.1662 11.2501C16.7791 11.2501 16.4367 11.2501 16.1497 11.278C15.8385 11.3082 15.5357 11.3751 15.2407 11.5422C14.9457 11.7092 14.7325 11.9344 14.5465 12.1857C14.3749 12.4174 14.1988 12.711 13.9996 13.043L13.9521 13.1222C13.8654 13.2668 13.793 13.3872 13.7284 13.4906C13.6676 13.3849 13.5999 13.2618 13.5186 13.1141L11.8092 10.006C11.6551 9.7256 11.5015 9.44626 11.3458 9.24147C11.1756 9.01775 10.8839 8.72194 10.4164 8.6967C9.94887 8.67146 9.62698 8.93414 9.43373 9.13823C9.25683 9.32506 9.0741 9.58625 8.89069 9.84841L8.58131 10.2904C8.35416 10.6149 8.21175 10.8171 8.08848 10.9629C7.975 11.0971 7.91193 11.1411 7.86538 11.1653C7.81882 11.1896 7.74663 11.216 7.57159 11.232C7.38144 11.2494 7.13413 11.2501 6.73803 11.2501H6C5.58579 11.2501 5.25 11.5859 5.25 12.0001C5.25 12.4143 5.58579 12.7501 6 12.7501L6.76812 12.7501C7.12509 12.7501 7.44153 12.7501 7.70801 12.7258C7.99707 12.6994 8.27904 12.6411 8.55809 12.4958C8.83714 12.3505 9.04661 12.153 9.234 11.9313C9.40676 11.7269 9.58821 11.4677 9.79291 11.1752L10.0932 10.7463Z" fill="#bf616a"/>
  </svg>
)

export const A4Config = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M20 18C20 20.2091 16.4183 22 12 22C7.58172 22 4 20.2091 4 18V13.974C4.50221 14.5906 5.21495 15.1029 6.00774 15.4992C7.58004 16.2854 9.69967 16.75 12 16.75C14.3003 16.75 16.42 16.2854 17.9923 15.4992C18.7851 15.1029 19.4978 14.5906 20 13.974V18Z" fill="#81a1c1"/>
    <path d="M12 10.75C14.3003 10.75 16.42 10.2854 17.9923 9.49925C18.7851 9.10285 19.4978 8.59059 20 7.97397V12C20 12.5 18.2143 13.5911 17.3214 14.1576C15.9983 14.8192 14.118 15.25 12 15.25C9.88205 15.25 8.00168 14.8192 6.67856 14.1576C5.5 13.5683 4 12.5 4 12V7.97397C4.50221 8.59059 5.21495 9.10285 6.00774 9.49925C7.58004 10.2854 9.69967 10.75 12 10.75Z" fill="#81a1c1"/>
    <path d="M17.3214 8.15761C15.9983 8.81917 14.118 9.25 12 9.25C9.88205 9.25 8.00168 8.81917 6.67856 8.15761C6.16384 7.95596 5.00637 7.31492 4.2015 6.27935C4.06454 6.10313 4.00576 5.87853 4.03988 5.65798C4.06283 5.50969 4.0948 5.35695 4.13578 5.26226C4.82815 3.40554 8.0858 2 12 2C15.9142 2 19.1718 3.40554 19.8642 5.26226C19.9052 5.35695 19.9372 5.50969 19.9601 5.65798C19.9942 5.87853 19.9355 6.10313 19.7985 6.27935C18.9936 7.31492 17.8362 7.95596 17.3214 8.15761Z" fill="#81a1c1"/>
  </svg>
)

export const A4Router = (props: IconProps) => (
  <svg style={{maxHeight: props.maxHeight}} viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path d="M12.1091 7.43399C12.823 5.71031 14.521 4.5 16.4998 4.5C18.4787 4.5 20.1767 5.71031 20.8905 7.43399C21.049 7.81668 21.4878 7.99844 21.8704 7.83995C22.2531 7.68146 22.4349 7.24275 22.2764 6.86005C21.3385 4.5954 19.1064 3 16.4998 3C13.8933 3 11.6612 4.5954 10.7233 6.86005C10.5648 7.24275 10.7466 7.68146 11.1292 7.83995C11.5119 7.99844 11.9507 7.81668 12.1091 7.43399Z" fill="#8fbcbb"/>
    <path fill-rule="evenodd" clip-rule="evenodd" d="M2.58579 12.3358C2 12.9216 2 13.8644 2 15.75C2 17.6356 2 18.5784 2.58579 19.1642C3.17157 19.75 4.11438 19.75 6 19.75H18C19.8856 19.75 20.8284 19.75 21.4142 19.1642C22 18.5784 22 17.6356 22 15.75C22 13.8644 22 12.9216 21.4142 12.3358C20.8284 11.75 19.8856 11.75 18 11.75H17.25V8.75C17.25 8.33579 16.9142 8 16.5 8C16.0858 8 15.75 8.33579 15.75 8.75V11.75H6C4.11438 11.75 3.17157 11.75 2.58579 12.3358ZM6 16.75C6.55228 16.75 7 16.3023 7 15.75C7 15.1977 6.55228 14.75 6 14.75C5.44772 14.75 5 15.1977 5 15.75C5 16.3023 5.44772 16.75 6 16.75ZM9 16.75C9.55228 16.75 10 16.3023 10 15.75C10 15.1977 9.55228 14.75 9 14.75C8.44772 14.75 8 15.1977 8 15.75C8 16.3023 8.44772 16.75 9 16.75Z" fill="#81a1c1"/>
    <path d="M14.3403 8.6182C14.6133 7.68244 15.4782 7 16.5006 7C17.5229 7 18.3878 7.68244 18.6609 8.6182C18.7769 9.01583 19.1933 9.24411 19.5909 9.12808C19.9886 9.01205 20.2168 8.59564 20.1008 8.19801C19.6461 6.63969 18.2074 5.5 16.5006 5.5C14.7938 5.5 13.3551 6.63969 12.9003 8.19801C12.7843 8.59564 13.0126 9.01205 13.4102 9.12808C13.8078 9.24411 14.2243 9.01583 14.3403 8.6182Z" fill="#88c0d0"/>
  </svg>
)
