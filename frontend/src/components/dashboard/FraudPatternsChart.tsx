import Chart from "react-apexcharts";
import { ApexOptions } from "apexcharts";

interface FraudPatternsProps {
  patterns?: Array<{
    type: string;
    count: number;
  }>;
}

export default function FraudPatternsChart({ patterns = [] }: FraudPatternsProps) {
  const hasData = patterns && patterns.length > 0;

  const categories = hasData 
    ? patterns.map(p => p.type)
    : ['High-Value', 'Unusual Time', 'Multiple Locations', 'Velocity', 'Other'];
    
  const data = hasData 
    ? patterns.map(p => p.count)
    : [12, 8, 15, 5, 3];

  const options: ApexOptions = {
    chart: {
      fontFamily: "Outfit, sans-serif",
      type: "bar",
      toolbar: {
        show: false,
      },
    },
    colors: ["#465FFF"],
    plotOptions: {
      bar: {
        horizontal: false,
        columnWidth: "55%",
        borderRadius: 8,
      },
    },
    dataLabels: {
      enabled: false,
    },
    stroke: {
      show: true,
      width: 2,
      colors: ["transparent"],
    },
    xaxis: {
      categories: categories,
      axisBorder: {
        show: false,
      },
      axisTicks: {
        show: false,
      },
      labels: {
        style: {
          fontSize: "12px",
        },
        rotate: -45,
      },
    },
    yaxis: {
      title: {
        text: "Number of Patterns",
        style: {
          fontSize: "12px",
          fontWeight: 400,
        },
      },
      labels: {
        formatter: function (value) {
          return Math.round(value).toString();
        },
      },
    },
    fill: {
      opacity: 1,
    },
    tooltip: {
      y: {
        formatter: function (value) {
          return `${value} patterns detected`;
        },
      },
    },
    grid: {
      xaxis: {
        lines: {
          show: false,
        },
      },
      yaxis: {
        lines: {
          show: true,
        },
      },
    },
  };

  const series = [
    {
      name: "Fraud Patterns",
      data: data,
    },
  ];

  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
          Fraud Pattern Detection
        </h3>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          Detected fraud patterns by type
        </p>
      </div>

      <div id="fraud-patterns-chart">
        <Chart options={options} series={series} type="bar" height={300} />
      </div>

      <div className="mt-4 flex items-center justify-between rounded-lg bg-gray-50 p-3 dark:bg-gray-800">
        <div className="flex items-center gap-2">
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-red-100 dark:bg-red-500/10">
            <svg className="h-4 w-4 text-red-600 dark:text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
            </svg>
          </div>
          <div>
            <p className="text-sm font-medium text-gray-900 dark:text-white">Total Patterns</p>
            <p className="text-xs text-gray-500 dark:text-gray-400">Across all types</p>
          </div>
        </div>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">
          {data.reduce((a, b) => a + b, 0)}
        </p>
      </div>
    </div>
  );
}
